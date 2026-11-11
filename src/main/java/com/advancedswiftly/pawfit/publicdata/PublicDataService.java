package com.advancedswiftly.pawfit.publicdata;

import com.advancedswiftly.pawfit.global.error.BusinessException;
import com.advancedswiftly.pawfit.global.error.ErrorCode;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
class PublicDataService {
    private static final int PAGE_SIZE = 20;
    private static final String RUNNING = "RUNNING";
    private final JdbcTemplate db;
    private final PetTourClient client;
    private final TaskExecutor executor;
    private final TransactionTemplate transaction;

    PublicDataService(JdbcTemplate db, PetTourClient client, PlatformTransactionManager manager,
            @Qualifier("publicDataExecutor") TaskExecutor executor) {
        this.db = db;
        this.client = client;
        this.executor = executor;
        this.transaction = new TransactionTemplate(manager);
    }

    public Sources sources() {
        return new Sources(db.query("""
                SELECT s.source_id, s.name,
                       (SELECT MAX(r.started_at) FROM public_data_sync_run r
                        WHERE r.source_id=s.source_id AND r.status='SUCCESS'),
                       COALESCE((SELECT status FROM public_data_sync_run r
                        WHERE r.source_id=s.source_id ORDER BY run_id DESC LIMIT 1), 'NEVER_SYNCED')
                FROM public_data_source s ORDER BY s.source_id
                """, (rs, index) -> new Source(rs.getLong(1), rs.getString(2),
                rs.getObject(3, OffsetDateTime.class), rs.getString(4))));
    }

    public Started start(long sourceId, boolean fullSync) {
        Job job = transaction.execute(ignored -> claim(sourceId, fullSync));
        try {
            executor.execute(() -> run(job));
        } catch (org.springframework.core.task.TaskRejectedException exception) {
            fail(job);
            throw new BusinessException(ErrorCode.PUBLIC_DATA_UNAVAILABLE);
        }
        return new Started(sourceId, RUNNING, job.startedAt());
    }

    private Job claim(long sourceId, boolean fullSync) {
        String code = lockSource(sourceId);
        OffsetDateTime now = Instant.now().atOffset(ZoneOffset.UTC);
        db.update("""
                UPDATE public_data_sync_run SET status='FAILED', failed_count=1
                WHERE source_id=? AND status='RUNNING' AND started_at < ?
                """, sourceId, now.minus(Duration.ofHours(1)));
        if (Boolean.TRUE.equals(db.queryForObject("""
                SELECT EXISTS(SELECT 1 FROM public_data_sync_run WHERE source_id=? AND status='RUNNING')
                """, Boolean.class, sourceId))) {
            throw new BusinessException(ErrorCode.SYNC_ALREADY_RUNNING);
        }
        if (!client.configured()) throw new BusinessException(ErrorCode.PUBLIC_DATA_UNAVAILABLE);
        OffsetDateTime since = fullSync ? null : db.queryForObject("""
                SELECT MAX(started_at) FROM public_data_sync_run
                WHERE source_id=? AND status='SUCCESS' AND failed_count=0
                """, OffsetDateTime.class, sourceId);
        var key = new org.springframework.jdbc.support.GeneratedKeyHolder();
        db.update(connection -> {
            var statement = connection.prepareStatement("""
                    INSERT INTO public_data_sync_run(source_id, started_at, status) VALUES (?, ?, 'RUNNING')
                    """, new String[] {"run_id"});
            statement.setLong(1, sourceId);
            statement.setObject(2, now);
            return statement;
        }, key);
        return new Job(key.getKey().longValue(), sourceId, code, now, since == null ? null : since.toInstant());
    }

    private void run(Job job) {
        try {
            PetTourClient.Batch batch = client.collect(job.since());
            transaction.executeWithoutResult(ignored -> {
                lockSource(job.sourceId());
                String status = db.queryForObject(
                        "SELECT status FROM public_data_sync_run WHERE run_id=?", String.class, job.id());
                if (!RUNNING.equals(status)) return;
                int upserted = 0;
                for (var row : batch.rows()) {
                    upserted += db.update("""
                            INSERT INTO public_facility(source_code, external_id, name, address, category,
                                                        latitude, longitude, region_id, visible)
                            VALUES (?, ?, ?, ?, ?, ?, ?, (SELECT region_id FROM region WHERE region_id=?), FALSE)
                            ON CONFLICT (source_code, external_id) DO UPDATE
                            SET name=EXCLUDED.name, address=EXCLUDED.address, category=EXCLUDED.category,
                                latitude=EXCLUDED.latitude, longitude=EXCLUDED.longitude,
                                region_id=COALESCE(EXCLUDED.region_id, public_facility.region_id)
                            WHERE NOT public_facility.operator_corrected
                            """, job.code(), row.id(), row.name(), row.address(), row.category(),
                            row.latitude(), row.longitude(), row.regionId());
                }
                db.update("""
                        UPDATE public_data_sync_run SET status='SUCCESS', upserted_count=?, failed_count=?
                        WHERE run_id=?
                        """, upserted, batch.failed(), job.id());
            });
        } catch (RuntimeException exception) {
            // 요청 URI에는 인증키가 포함될 수 있으므로 예외 원문을 기록하지 않습니다.
            LoggerFactory.getLogger(PublicDataService.class).warn("공공데이터 동기화 실패. runId={}, type={}",
                    job.id(), exception.getClass().getSimpleName());
            fail(job);
        }
    }

    private void fail(Job job) {
        db.update("""
                UPDATE public_data_sync_run SET status='FAILED', failed_count=1
                WHERE run_id=? AND status='RUNNING'
                """, job.id());
    }

    public Items<Run> runs(long sourceId, int page) {
        if (!Boolean.TRUE.equals(db.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM public_data_source WHERE source_id=?)", Boolean.class, sourceId))) {
            throw invalid();
        }
        return new Items<>(db.query("""
                SELECT started_at, status, upserted_count, failed_count FROM public_data_sync_run
                WHERE source_id=? ORDER BY run_id DESC LIMIT ? OFFSET ?
                """, (rs, index) -> new Run(rs.getObject(1, OffsetDateTime.class), rs.getString(2),
                rs.getInt(3), rs.getInt(4)), sourceId, PAGE_SIZE, (long) page * PAGE_SIZE));
    }

    public Items<Facility> facilities(Long sourceId, Long regionId, String keyword, int page) {
        var args = new java.util.ArrayList<>();
        var sql = new StringBuilder("""
                SELECT f.facility_id, f.name, s.source_id, f.latitude, f.longitude, f.category, f.visible
                FROM public_facility f LEFT JOIN public_data_source s ON f.source_code=s.source_code
                WHERE 1=1
                """);
        if (sourceId != null) {
            sql.append(" AND s.source_id=?");
            args.add(sourceId);
        }
        if (regionId != null) {
            sql.append(" AND f.region_id=?");
            args.add(regionId);
        }
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND LOWER(f.name) LIKE LOWER(?) ESCAPE '!'");
            args.add("%" + keyword.strip().replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%");
        }
        sql.append(" ORDER BY f.facility_id LIMIT ? OFFSET ?");
        args.add(PAGE_SIZE);
        args.add((long) page * PAGE_SIZE);
        return new Items<>(db.query(sql.toString(), (rs, index) -> new Facility(rs.getLong(1),
                rs.getString(2), rs.getObject(3, Long.class), rs.getBigDecimal(4), rs.getBigDecimal(5),
                rs.getString(6), rs.getBoolean(7)), args.toArray()));
    }

    public Corrected correct(long id, PublicDataController.Correction request) {
        if ((request.name() != null && request.name().isBlank())
                || (request.category() != null && request.category().isBlank())) throw invalid();
        return transaction.execute(ignored -> {
            boolean corrected = request.name() != null || request.latitude() != null
                    || request.longitude() != null || request.category() != null;
            if (db.update("""
                    UPDATE public_facility SET name=COALESCE(?, name), latitude=COALESCE(?, latitude),
                    longitude=COALESCE(?, longitude), category=COALESCE(?, category),
                    visible=COALESCE(?, visible), operator_corrected=operator_corrected OR ?
                    WHERE facility_id=?
                    """, request.name(), request.latitude(), request.longitude(), request.category(),
                    request.visible(), corrected, id) == 0) throw invalid();
            return new Corrected(id, Boolean.TRUE.equals(db.queryForObject(
                    "SELECT visible FROM public_facility WHERE facility_id=?", Boolean.class, id)));
        });
    }

    private String lockSource(long sourceId) {
        try {
            return db.queryForObject(
                    "SELECT source_code FROM public_data_source WHERE source_id=? FOR UPDATE", String.class, sourceId);
        } catch (EmptyResultDataAccessException exception) {
            throw invalid();
        }
    }

    private BusinessException invalid() {
        return new BusinessException(ErrorCode.INVALID_REQUEST);
    }

    record Sources(List<Source> sources) {}
    record Source(long sourceId, String name, OffsetDateTime lastSyncedAt, String status) {}
    record Started(long sourceId, String status, OffsetDateTime startedAt) {}
    record Items<T>(List<T> items) {}
    record Run(OffsetDateTime startedAt, String status, int upsertedCount, int failedCount) {}
    record Facility(long facilityId, String name, Long sourceId, BigDecimal latitude,
                    BigDecimal longitude, String category, boolean visible) {}
    record Corrected(long facilityId, boolean visible) {}
    private record Job(long id, long sourceId, String code, OffsetDateTime startedAt, Instant since) {}
}
