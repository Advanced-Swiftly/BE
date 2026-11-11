package com.advancedswiftly.pawfit.admin.application;

import com.advancedswiftly.pawfit.admin.api.dto.AdminVerificationDetailResponse;
import com.advancedswiftly.pawfit.admin.api.dto.AdminVerificationListItem;
import com.advancedswiftly.pawfit.admin.api.dto.AdminVerificationListResponse;
import com.advancedswiftly.pawfit.admin.api.dto.VerificationReviewRequest;
import com.advancedswiftly.pawfit.admin.api.dto.VerificationReviewResponse;
import com.advancedswiftly.pawfit.admin.domain.StoreVerificationHistory;
import com.advancedswiftly.pawfit.admin.infrastructure.StoreVerificationHistoryRepository;
import com.advancedswiftly.pawfit.business.domain.ReviewAction;
import com.advancedswiftly.pawfit.business.domain.Store;
import com.advancedswiftly.pawfit.business.domain.StoreManager;
import com.advancedswiftly.pawfit.business.domain.StoreVerification;
import com.advancedswiftly.pawfit.business.domain.StoreVerificationStatus;
import com.advancedswiftly.pawfit.business.domain.VerificationStatus;
import com.advancedswiftly.pawfit.business.infrastructure.StoreManagerRepository;
import com.advancedswiftly.pawfit.business.infrastructure.StoreVerificationRepository;
import com.advancedswiftly.pawfit.global.error.BusinessException;
import com.advancedswiftly.pawfit.global.error.ErrorCode;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** 운영자의 사업자 인증 심사 목록·상세·처리. */
@Service
public class AdminVerificationService {

    public static final int PAGE_SIZE = 20;

    private final JdbcTemplate jdbcTemplate;
    private final StoreVerificationRepository verificationRepository;
    private final StoreVerificationHistoryRepository historyRepository;
    private final StoreManagerRepository managerRepository;

    public AdminVerificationService(
            JdbcTemplate jdbcTemplate,
            StoreVerificationRepository verificationRepository,
            StoreVerificationHistoryRepository historyRepository,
            StoreManagerRepository managerRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.verificationRepository = verificationRepository;
        this.historyRepository = historyRepository;
        this.managerRepository = managerRepository;
    }

    /** keyword는 상호명 또는 사업자번호(하이픈 무시)에 포함된 것을 찾는다. */
    @Transactional(readOnly = true)
    public AdminVerificationListResponse list(VerificationStatus status, String keyword, int page) {
        StringBuilder sql = new StringBuilder(
                "SELECT verification_id, store_id, business_name, business_number, status, created_at "
                        + "FROM store_verification WHERE 1 = 1");
        List<Object> args = new ArrayList<>();
        if (status != null) {
            sql.append(" AND status = ?");
            args.add(status.name());
        }
        if (StringUtils.hasText(keyword)) {
            String trimmed = keyword.trim();
            sql.append(" AND (business_name LIKE ? OR business_number LIKE ?)");
            args.add("%" + trimmed + "%");
            args.add("%" + trimmed.replace("-", "") + "%");
        }
        sql.append(" ORDER BY verification_id DESC LIMIT ? OFFSET ?");
        args.add(PAGE_SIZE);
        args.add((page - 1) * PAGE_SIZE);
        List<AdminVerificationListItem> items = jdbcTemplate.query(
                sql.toString(),
                (rs, rowNum) -> new AdminVerificationListItem(
                        rs.getLong(1),
                        rs.getLong(2),
                        rs.getString(3),
                        rs.getString(4),
                        VerificationStatus.valueOf(rs.getString(5)),
                        rs.getTimestamp(6).toInstant()),
                args.toArray());
        return new AdminVerificationListResponse(items);
    }

    @Transactional(readOnly = true)
    public AdminVerificationDetailResponse get(Long verificationId) {
        StoreVerification verification = find(verificationId);
        Store store = verification.getStore();
        List<AdminVerificationDetailResponse.StatusHistory> history = new ArrayList<>();
        history.add(new AdminVerificationDetailResponse.StatusHistory(
                VerificationStatus.RECEIVED, null, verification.getCreatedAt()));
        for (StoreVerificationHistory row : historyRepository.findByVerificationIdOrderByIdAsc(verificationId)) {
            history.add(new AdminVerificationDetailResponse.StatusHistory(
                    row.getStatus(), row.getComment(), row.getCreatedAt()));
        }
        return new AdminVerificationDetailResponse(
                verification.getId(),
                verification.getStatus(),
                store.getId(),
                store.getName(),
                store.getAddress(),
                store.getPhone(),
                verification.getBusinessNumber(),
                verification.getBusinessName(),
                verification.getRepresentativeName(),
                verification.getDocumentUrl(),
                verification.getSubmittedBy(),
                verification.getReviewNote(),
                history);
    }

    /**
     * 승인하면 업체가 인증 완료(VERIFIED)가 되고 요청자가 업체 관리자로 연결된다.
     * 반려하면 업체는 REJECTED가 된다. 허용되지 않는 전이는 INVALID_STATUS_TRANSITION(409).
     */
    @Transactional
    public VerificationReviewResponse review(Long operatorId, Long verificationId, VerificationReviewRequest request) {
        StoreVerification verification = find(verificationId);
        ReviewAction action = ReviewAction.valueOf(request.action().name());
        VerificationStatus target = verification.getStatus().afterReview(action);
        if (target == null) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
        boolean needsComment = action == ReviewAction.NEEDS_INFO || action == ReviewAction.REJECT;
        if (needsComment && !StringUtils.hasText(request.comment())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "보완·반려 사유는 필수입니다.");
        }

        Store store = verification.getStore();
        if (action == ReviewAction.APPROVE) {
            store.changeVerificationStatus(StoreVerificationStatus.VERIFIED);
            assignManager(store.getId(), verification.getSubmittedBy());
        } else if (action == ReviewAction.REJECT) {
            store.changeVerificationStatus(StoreVerificationStatus.REJECTED);
        }
        verification.review(target, request.comment(), operatorId);
        historyRepository.save(new StoreVerificationHistory(verificationId, target, request.comment(), operatorId));
        return new VerificationReviewResponse(verificationId, target, store.getId());
    }

    private void assignManager(Long storeId, Long userId) {
        managerRepository.findById(storeId).ifPresentOrElse(
                manager -> manager.changeUser(userId),
                () -> managerRepository.save(new StoreManager(storeId, userId)));
    }

    private StoreVerification find(Long verificationId) {
        return verificationRepository.findById(verificationId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }
}
