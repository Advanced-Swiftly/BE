package com.advancedswiftly.pawfit.admin.application;

import com.advancedswiftly.pawfit.admin.api.dto.DashboardSummaryResponse;
import com.advancedswiftly.pawfit.admin.api.dto.PolicyHistoryItem;
import com.advancedswiftly.pawfit.admin.api.dto.PolicyHistoryResponse;
import com.advancedswiftly.pawfit.admin.api.dto.VaccinationReviewResponse;
import com.advancedswiftly.pawfit.business.infrastructure.StorePolicyRepository;
import com.advancedswiftly.pawfit.business.infrastructure.StoreRepository;
import com.advancedswiftly.pawfit.global.error.BusinessException;
import com.advancedswiftly.pawfit.global.error.ErrorCode;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 운영자 대시보드 요약, 정책 변경 이력, 예방접종 검토 상세. */
@Service
public class AdminOverviewService {

    public static final int PAGE_SIZE = 20;

    private final JdbcTemplate jdbcTemplate;
    private final StoreRepository storeRepository;
    private final StorePolicyRepository policyRepository;

    public AdminOverviewService(
            JdbcTemplate jdbcTemplate, StoreRepository storeRepository, StorePolicyRepository policyRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.storeRepository = storeRepository;
        this.policyRepository = policyRepository;
    }

    /** from/to는 함께 주거나 함께 생략한다. 날짜 경계는 UTC 기준이다. */
    @Transactional(readOnly = true)
    public DashboardSummaryResponse dashboardSummary(LocalDate from, LocalDate to) {
        if ((from == null) != (to == null)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "from과 to는 함께 전달해야 합니다.");
        }
        if (from != null && to.isBefore(from)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "to는 from보다 빠를 수 없습니다.");
        }
        long pendingVerifications = count(
                "SELECT COUNT(*) FROM store_verification WHERE status IN ('RECEIVED', 'IN_REVIEW')");
        long pendingConditionRequests = count(
                "SELECT COUNT(*) FROM condition_request WHERE status IN ('RECEIVED', 'IN_REVIEW')");
        long activeStores = count("SELECT COUNT(*) FROM store WHERE verification_status = 'VERIFIED'");
        long totalVisits = from == null
                ? count("SELECT COUNT(*) FROM visit")
                : jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM visit WHERE visited_at >= ? AND visited_at < ?",
                        Long.class,
                        Timestamp.from(from.atStartOfDay(ZoneOffset.UTC).toInstant()),
                        Timestamp.from(to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()));
        return new DashboardSummaryResponse(pendingVerifications, pendingConditionRequests, activeStores, totalVisits);
    }

    @Transactional(readOnly = true)
    public PolicyHistoryResponse policyHistory(Long storeId, int page) {
        if (!storeRepository.existsById(storeId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        List<PolicyHistoryItem> items = policyRepository
                .findByStoreIdOrderByVersionDesc(storeId, PageRequest.of(page - 1, PAGE_SIZE))
                .map(policy -> new PolicyHistoryItem(
                        policy.getId(),
                        policy.getVersion(),
                        policy.getChangedBy(),
                        policy.getRequestId(),
                        policy.getSourceText(),
                        policy.getCreatedAt()))
                .getContent();
        return new PolicyHistoryResponse(items);
    }

    @Transactional(readOnly = true)
    public VaccinationReviewResponse vaccinationReview(Long recordId) {
        List<Object[]> records = jdbcTemplate.query(
                "SELECT dog_id, status FROM vaccination_record WHERE record_id = ?",
                (rs, rowNum) -> new Object[] {rs.getLong(1), rs.getString(2)},
                recordId);
        if (records.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        List<VaccinationReviewResponse.Document> documents = jdbcTemplate.query(
                "SELECT document_id, object_path FROM vaccination_document WHERE record_id = ? ORDER BY document_id",
                (rs, rowNum) -> new VaccinationReviewResponse.Document(rs.getLong(1), rs.getString(2)),
                recordId);
        List<VaccinationReviewResponse.Item> items = jdbcTemplate.query(
                "SELECT item_id, source, vaccine_code, vaccinated_at, confidence FROM vaccination_item "
                        + "WHERE record_id = ? ORDER BY item_id",
                (rs, rowNum) -> new VaccinationReviewResponse.Item(
                        rs.getLong(1),
                        rs.getString(2),
                        rs.getString(3),
                        rs.getDate(4).toLocalDate(),
                        rs.getObject(5, Double.class)),
                recordId);
        return new VaccinationReviewResponse(
                recordId, (Long) records.get(0)[0], (String) records.get(0)[1], documents, items);
    }

    private long count(String sql) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class);
        return value == null ? 0L : value;
    }
}
