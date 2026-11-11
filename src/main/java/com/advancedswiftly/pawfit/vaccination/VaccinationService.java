package com.advancedswiftly.pawfit.vaccination;

import com.advancedswiftly.pawfit.global.ai.AiService;
import com.advancedswiftly.pawfit.global.ai.MaskedDocument;
import com.advancedswiftly.pawfit.global.error.BusinessException;
import com.advancedswiftly.pawfit.global.error.ErrorCode;
import com.advancedswiftly.pawfit.global.storage.TransactionalStorageService;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

@Service
class VaccinationService {

    private final JdbcTemplate db;
    private final TransactionalStorageService storage;
    private final AiService ai;
    private final ApplicationEventPublisher events;
    private final TaskExecutor executor;
    private final TransactionTemplate transaction;

    VaccinationService(JdbcTemplate db, TransactionalStorageService storage, AiService ai,
            ApplicationEventPublisher events, @Qualifier("vaccinationExecutor") TaskExecutor executor,
            PlatformTransactionManager manager) {
        this.db = db;
        this.storage = storage;
        this.ai = ai;
        this.events = events;
        this.executor = executor;
        this.transaction = new TransactionTemplate(manager);
        this.transaction.setPropagationBehaviorName("PROPAGATION_REQUIRES_NEW");
    }

    @Transactional
    public Upload upload(long userId, Long dogId, Long recordId, List<MultipartFile> files) {
        if (recordId != null) {
            dogId = ownedRecord(userId, recordId).dogId();
        }
        lockDog(userId, dogId);
        Record previous = byDog(dogId);
        int revision = 1;
        if (recordId == null) {
            if (previous != null) throw error(ErrorCode.RESUBMIT_NOT_ALLOWED);
            recordId = insertReturningId("""
                    INSERT INTO vaccination_record(dog_id, status) VALUES (?, 'ANALYZING')
                    """, "record_id", dogId);
        } else {
            if (previous == null || previous.id() != recordId
                    || !List.of("ANALYZED", "NEEDS_MANUAL_REVIEW", "REJECTED").contains(previous.status())) {
                throw error(ErrorCode.RESUBMIT_NOT_ALLOWED);
            }
            revision = previous.revision() + 1;
            db.update("UPDATE vaccination_record SET status='ANALYZING', revision=? WHERE record_id=?",
                    revision, recordId);
            db.queryForList("SELECT object_path FROM vaccination_document WHERE record_id=?", String.class,
                    recordId).forEach(storage::deleteAfterCommit);
            db.update("DELETE FROM vaccination_document WHERE record_id=?", recordId);
            db.update("DELETE FROM vaccination_item WHERE record_id=?", recordId);
        }
        if (files.isEmpty() || files.size() > 3) throw error(ErrorCode.INVALID_DOCUMENT_FORMAT);
        List<Document> documents = new ArrayList<>();
        List<MaskedDocument> inputs = new ArrayList<>();
        for (MultipartFile file : files) {
            String type = file.getContentType();
            String extension = switch (type == null ? "" : type) {
                case "image/jpeg" -> ".jpg";
                case "image/png" -> ".png";
                case "application/pdf" -> ".pdf";
                default -> throw error(ErrorCode.INVALID_DOCUMENT_FORMAT);
            };
            String path = "vaccinations/" + userId + "/" + dogId + "/" + UUID.randomUUID() + extension;
            try {
                byte[] bytes = file.getBytes();
                storage.upload(path, bytes, type);
                inputs.add(new MaskedDocument(bytes, type));
            } catch (IOException exception) {
                throw error(ErrorCode.INVALID_DOCUMENT_FORMAT);
            } catch (BusinessException exception) {
                if (List.of(ErrorCode.INVALID_FILE, ErrorCode.FILE_TOO_LARGE, ErrorCode.UNSUPPORTED_FILE_TYPE)
                        .contains(exception.getErrorCode())) {
                    throw error(ErrorCode.INVALID_DOCUMENT_FORMAT);
                }
                throw exception;
            }
            documents.add(new Document(insertReturningId("""
                    INSERT INTO vaccination_document(record_id, object_path) VALUES (?, ?)
                    """, "document_id", recordId, path)));
        }
        dogStatus(dogId, "PENDING");
        events.publishEvent(new AnalysisJob(recordId, dogId, revision, List.copyOf(inputs)));
        return new Upload(recordId, "ANALYZING", documents);
    }

    @TransactionalEventListener
    public void schedule(AnalysisJob job) {
        try {
            executor.execute(() -> runAnalysis(job));
        } catch (org.springframework.core.task.TaskRejectedException exception) {
            finish(job, List.of(), "NEEDS_MANUAL_REVIEW");
        }
    }

    private void runAnalysis(AnalysisJob job) {
        List<Extracted> items = new ArrayList<>();
        boolean manual = false;
        for (MaskedDocument document : job.documents()) {
            try {
                var result = ai.analyzeVaccination(document).aiAnalysis().extraction();
                if (result == null || result.vaccinations().isEmpty()) {
                    manual = true;
                    continue;
                }
                for (var item : result.vaccinations()) {
                    try {
                        String code = item.name().strip().toUpperCase(Locale.ROOT);
                        code = switch (code) {
                            case "광견병", "광견병 백신" -> "RABIES";
                            case "종합백신", "종합 백신" -> "DHPPL";
                            default -> code;
                        };
                        LocalDate date = LocalDate.parse(item.administeredDate());
                        if (!code.matches("[A-Z][A-Z0-9_]{0,79}") || date.isAfter(LocalDate.now())) {
                            manual = true;
                            continue;
                        }
                        Double confidence = result.confidence() >= 0 && result.confidence() <= 1
                                ? result.confidence() : null;
                        items.add(new Extracted(code, date, confidence));
                    } catch (IllegalArgumentException | java.time.DateTimeException | NullPointerException exception) {
                        manual = true;
                    }
                }
            } catch (RuntimeException exception) {
                manual = true;
            }
        }
        finish(job, items.stream().distinct().toList(),
                manual || items.isEmpty() ? "NEEDS_MANUAL_REVIEW" : "ANALYZED");
    }

    private void finish(AnalysisJob job, List<Extracted> items, String status) {
        transaction.executeWithoutResult(ignored -> {
            // 반려견을 먼저 잠가 수정·재제출과 동일한 잠금 순서를 유지합니다.
            if (db.queryForList("SELECT dog_id FROM dog WHERE dog_id=? FOR UPDATE",
                    Long.class, job.dogId()).isEmpty()) return;
            if (db.update("""
                    UPDATE vaccination_record SET status=?
                    WHERE record_id=? AND revision=? AND status='ANALYZING'
                    """, status, job.recordId(), job.revision()) == 0) return;
            for (Extracted item : items) {
                insertItem(job.recordId(), "AI", item.code(), item.date(), item.confidence());
            }
            dogStatus(job.dogId(), status.equals("ANALYZED") ? "PENDING" : status);
        });
    }

    @Transactional(readOnly = true)
    public Result current(long userId, long dogId) {
        if (db.queryForList("SELECT dog_id FROM dog WHERE dog_id=? AND user_id=?",
                Long.class, dogId, userId).isEmpty()) throw error(ErrorCode.DOG_NOT_FOUND);
        Record record = byDog(dogId);
        return record == null ? new Result(null, "NOT_SUBMITTED", List.of()) : result(record, false);
    }

    @Transactional(readOnly = true)
    public Result analysis(long userId, long recordId) {
        return result(ownedRecord(userId, recordId), true);
    }

    @Transactional
    public State edit(long userId, long recordId, List<VaccinationController.Item> items) {
        Record record = ownedRecord(userId, recordId);
        lockDog(userId, record.dogId());
        record = ownedRecord(userId, recordId);
        if (!List.of("ANALYZED", "NEEDS_MANUAL_REVIEW").contains(record.status())) {
            throw error(ErrorCode.RECORD_NOT_EDITABLE);
        }
        for (var item : items.stream().distinct().toList()) {
            insertItem(recordId, "GUARDIAN", item.vaccineCode(), item.vaccinatedAt(), null);
        }
        db.update("UPDATE vaccination_record SET status='OPERATOR_REVIEW' WHERE record_id=?", recordId);
        dogStatus(record.dogId(), "PENDING");
        return new State(recordId, "OPERATOR_REVIEW");
    }

    private Result result(Record record, boolean analysis) {
        String source = analysis ? "AI" : switch (record.status()) {
            case "VERIFIED" -> "OPERATOR";
            case "OPERATOR_REVIEW" -> "GUARDIAN";
            default -> "AI";
        };
        List<Item> items = db.query("""
                SELECT item_id, vaccine_code, vaccinated_at, confidence FROM vaccination_item
                WHERE record_id=? AND source=? ORDER BY item_id
                """, (rs, row) -> new Item(rs.getLong(1), rs.getString(2),
                rs.getObject(3, LocalDate.class), rs.getObject(4, Double.class)), record.id(), source);
        return new Result(record.id(), record.status(), items);
    }

    private void insertItem(long recordId, String source, String code, LocalDate date, Double confidence) {
        db.update("""
                INSERT INTO vaccination_item(record_id, source, vaccine_code, vaccinated_at, confidence)
                VALUES (?, ?, ?, ?, ?)
                """, recordId, source, code, date, confidence);
    }

    private long insertReturningId(String sql, String column, Object... args) {
        var key = new GeneratedKeyHolder();
        db.update(connection -> {
            var statement = connection.prepareStatement(sql, new String[] {column});
            for (int index = 0; index < args.length; index++) {
                statement.setObject(index + 1, args[index]);
            }
            return statement;
        }, key);
        return java.util.Objects.requireNonNull(key.getKey()).longValue();
    }

    private Record ownedRecord(long userId, long recordId) {
        return db.query("""
                SELECT r.record_id, r.dog_id, r.status, r.revision
                FROM vaccination_record r JOIN dog d ON d.dog_id=r.dog_id
                WHERE r.record_id=? AND d.user_id=?
                """, (rs, row) -> new Record(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getInt(4)),
                recordId, userId).stream().findFirst().orElseThrow(() -> error(ErrorCode.INVALID_REQUEST));
    }

    private Record byDog(long dogId) {
        return db.query("SELECT record_id, dog_id, status, revision FROM vaccination_record WHERE dog_id=?",
                (rs, row) -> new Record(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getInt(4)),
                dogId).stream().findFirst().orElse(null);
    }

    private void lockDog(long userId, long dogId) {
        if (db.queryForList("SELECT dog_id FROM dog WHERE dog_id=? AND user_id=? FOR UPDATE",
                Long.class, dogId, userId).isEmpty()) throw error(ErrorCode.DOG_NOT_FOUND);
    }

    private void dogStatus(long dogId, String status) {
        db.update("UPDATE dog SET vaccination_status=?, updated_at=CURRENT_TIMESTAMP WHERE dog_id=?",
                status, dogId);
    }

    private BusinessException error(ErrorCode code) {
        return new BusinessException(code);
    }

    private record Record(long id, long dogId, String status, int revision) {}
    private record Extracted(String code, LocalDate date, Double confidence) {}
    record AnalysisJob(long recordId, long dogId, int revision, List<MaskedDocument> documents) {}
    record Document(long documentId) {}
    record State(long recordId, String status) {}
    record Upload(long recordId, String status, List<Document> documents) {}
    record Item(long itemId, String vaccineCode, LocalDate vaccinatedAt, Double confidence) {}
    record Result(Long recordId, String status, List<Item> items) {}
}
