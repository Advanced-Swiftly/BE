package com.advancedswiftly.pawfit.admin.application;

import com.advancedswiftly.pawfit.admin.api.dto.VaccinationReviewActionRequest;
import com.advancedswiftly.pawfit.admin.api.dto.VaccinationReviewActionResponse;
import com.advancedswiftly.pawfit.admin.api.dto.VaccinationReviewListItem;
import com.advancedswiftly.pawfit.admin.api.dto.VaccinationReviewListResponse;
import com.advancedswiftly.pawfit.admin.domain.VaccinationReview;
import com.advancedswiftly.pawfit.admin.infrastructure.VaccinationReviewRepository;
import com.advancedswiftly.pawfit.global.error.BusinessException;
import com.advancedswiftly.pawfit.global.error.ErrorCode;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** 운영자의 예방접종 검토 목록과 처리. 예방접종 테이블은 반려견 도메인 소유라 SQL로 다룬다. */
@Service
public class AdminVaccinationService {

    public static final int PAGE_SIZE = 20;

    /** 운영자가 처리할 수 있는 기록 상태. */
    public static final Set<String> REVIEWABLE = Set.of("ANALYZED", "NEEDS_MANUAL_REVIEW", "OPERATOR_REVIEW");

    private final JdbcTemplate jdbcTemplate;
    private final VaccinationReviewRepository reviewRepository;

    public AdminVaccinationService(JdbcTemplate jdbcTemplate, VaccinationReviewRepository reviewRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.reviewRepository = reviewRepository;
    }

    @Transactional(readOnly = true)
    public VaccinationReviewListResponse list(String status, int page) {
        if (status != null && !REVIEWABLE.contains(status)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "status 값이 올바르지 않습니다.");
        }
        StringBuilder sql = new StringBuilder(
                "SELECT r.record_id, r.dog_id, d.name, r.status FROM vaccination_record r "
                        + "LEFT JOIN dog d ON d.dog_id = r.dog_id WHERE ");
        List<Object> args = new ArrayList<>();
        if (status == null) {
            sql.append("r.status IN ('ANALYZED', 'NEEDS_MANUAL_REVIEW', 'OPERATOR_REVIEW')");
        } else {
            sql.append("r.status = ?");
            args.add(status);
        }
        sql.append(" ORDER BY r.record_id DESC LIMIT ? OFFSET ?");
        args.add(PAGE_SIZE);
        args.add((page - 1) * PAGE_SIZE);
        List<VaccinationReviewListItem> items = jdbcTemplate.query(
                sql.toString(),
                (rs, rowNum) -> new VaccinationReviewListItem(
                        rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getString(4)),
                args.toArray());
        return new VaccinationReviewListResponse(items);
    }

    /**
     * VERIFY → VERIFIED, NEEDS_MORE → NEEDS_MANUAL_REVIEW, REQUEST_REUPLOAD → REJECTED.
     * 반려견의 접종 상태도 같은 값으로 맞춘다.
     */
    @Transactional
    public VaccinationReviewActionResponse act(Long operatorId, Long recordId, VaccinationReviewActionRequest request) {
        List<Object[]> rows = jdbcTemplate.query(
                "SELECT dog_id, status FROM vaccination_record WHERE record_id = ?",
                (rs, rowNum) -> new Object[] {rs.getLong(1), rs.getString(2)},
                recordId);
        if (rows.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (!REVIEWABLE.contains((String) rows.get(0)[1])) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_STATE, "검토할 수 없는 상태입니다.");
        }
        Long dogId = (Long) rows.get(0)[0];
        String target = switch (request.action()) {
            case VERIFY -> "VERIFIED";
            case NEEDS_MORE -> "NEEDS_MANUAL_REVIEW";
            case REQUEST_REUPLOAD -> "REJECTED";
        };
        boolean needsComment = !"VERIFIED".equals(target);
        if (needsComment && !StringUtils.hasText(request.comment())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "사유는 필수입니다.");
        }

        if (request.items() != null && !request.items().isEmpty()) {
            if (!"VERIFIED".equals(target)) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "접종 항목은 인증 완료에서만 전달할 수 있습니다.");
            }
            jdbcTemplate.update("DELETE FROM vaccination_item WHERE record_id = ? AND source = 'OPERATOR'", recordId);
            for (VaccinationReviewActionRequest.Item item : request.items()) {
                jdbcTemplate.update(
                        "INSERT INTO vaccination_item (record_id, source, vaccine_code, vaccinated_at) "
                                + "VALUES (?, 'OPERATOR', ?, ?)",
                        recordId, item.vaccineCode().trim(), Date.valueOf(item.vaccinatedAt()));
            }
        }
        jdbcTemplate.update("UPDATE vaccination_record SET status = ? WHERE record_id = ?", target, recordId);
        jdbcTemplate.update("UPDATE dog SET vaccination_status = ? WHERE dog_id = ?", target, dogId);

        VaccinationReview review = reviewRepository
                .findById(recordId)
                .orElseGet(() -> new VaccinationReview(recordId));
        review.record(request.comment(), operatorId);
        reviewRepository.save(review);
        return new VaccinationReviewActionResponse(recordId, target);
    }
}
