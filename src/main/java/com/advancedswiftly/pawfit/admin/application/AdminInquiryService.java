package com.advancedswiftly.pawfit.admin.application;

import com.advancedswiftly.pawfit.admin.api.dto.AdminInquiryDetailResponse;
import com.advancedswiftly.pawfit.admin.api.dto.InquiryAnswerRequest;
import com.advancedswiftly.pawfit.admin.api.dto.InquiryAnswerResponse;
import com.advancedswiftly.pawfit.admin.api.dto.InquiryListItem;
import com.advancedswiftly.pawfit.admin.api.dto.InquiryListResponse;
import com.advancedswiftly.pawfit.admin.api.dto.InquiryUser;
import com.advancedswiftly.pawfit.admin.domain.InquiryAnswerAttachment;
import com.advancedswiftly.pawfit.admin.infrastructure.InquiryAnswerAttachmentRepository;
import com.advancedswiftly.pawfit.global.error.BusinessException;
import com.advancedswiftly.pawfit.global.error.ErrorCode;
import com.advancedswiftly.pawfit.inquiry.domain.InquiryCategory;
import com.advancedswiftly.pawfit.inquiry.domain.InquiryStatus;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 운영자의 사용자 문의 조회·답변. 문의 테이블은 문의 도메인 소유라 SQL로 다룬다. */
@Service
public class AdminInquiryService {

    public static final int PAGE_SIZE = 20;

    private final JdbcTemplate jdbcTemplate;
    private final InquiryAnswerAttachmentRepository answerAttachmentRepository;

    public AdminInquiryService(JdbcTemplate jdbcTemplate, InquiryAnswerAttachmentRepository answerAttachmentRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.answerAttachmentRepository = answerAttachmentRepository;
    }

    @Transactional(readOnly = true)
    public InquiryListResponse list(InquiryStatus status, InquiryCategory category, int page) {
        StringBuilder sql = new StringBuilder(
                "SELECT inquiry_id, category, title, status, created_at FROM inquiry WHERE 1 = 1");
        List<Object> args = new ArrayList<>();
        if (status != null) {
            sql.append(" AND status = ?");
            args.add(status.name());
        }
        if (category != null) {
            sql.append(" AND category = ?");
            args.add(category.name());
        }
        sql.append(" ORDER BY inquiry_id DESC LIMIT ? OFFSET ?");
        args.add(PAGE_SIZE);
        args.add((page - 1) * PAGE_SIZE);
        List<InquiryListItem> items = jdbcTemplate.query(
                sql.toString(),
                (rs, rowNum) -> new InquiryListItem(
                        rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), toInstant(rs.getTimestamp(5))),
                args.toArray());
        return new InquiryListResponse(items);
    }

    @Transactional(readOnly = true)
    public AdminInquiryDetailResponse get(Long inquiryId) {
        List<AdminInquiryDetailResponse> rows = jdbcTemplate.query(
                "SELECT i.inquiry_id, i.user_id, u.name, i.category, i.title, i.content, i.status, i.created_at, "
                        + "i.answer, i.answered_at FROM inquiry i JOIN app_user u ON u.user_id = i.user_id "
                        + "WHERE i.inquiry_id = ?",
                (rs, rowNum) -> new AdminInquiryDetailResponse(
                        rs.getLong(1),
                        new InquiryUser(rs.getLong(2), rs.getString(3)),
                        rs.getString(4),
                        rs.getString(5),
                        rs.getString(6),
                        rs.getString(7),
                        List.of(),
                        toInstant(rs.getTimestamp(8)),
                        rs.getString(9),
                        toInstant(rs.getTimestamp(10)),
                        List.of()),
                inquiryId);
        if (rows.isEmpty()) {
            throw new BusinessException(ErrorCode.INQUIRY_NOT_FOUND);
        }
        AdminInquiryDetailResponse row = rows.get(0);
        List<String> attachments = jdbcTemplate.queryForList(
                "SELECT attachment_url FROM inquiry_attachment WHERE inquiry_id = ? ORDER BY display_order",
                String.class,
                inquiryId);
        List<String> answerAttachments = jdbcTemplate.queryForList(
                "SELECT attachment_url FROM inquiry_answer_attachment WHERE inquiry_id = ? ORDER BY display_order",
                String.class,
                inquiryId);
        return new AdminInquiryDetailResponse(
                row.inquiryId(),
                row.user(),
                row.category(),
                row.title(),
                row.content(),
                row.status(),
                attachments,
                row.createdAt(),
                row.answer(),
                row.answeredAt(),
                answerAttachments);
    }

    /** 대기 중(WAITING) 문의에만 답변할 수 있다. 이미 답변된 문의는 409. */
    @Transactional
    public InquiryAnswerResponse answer(Long inquiryId, InquiryAnswerRequest request) {
        Instant now = Instant.now();
        int updated = jdbcTemplate.update(
                "UPDATE inquiry SET answer = ?, status = 'ANSWERED', answered_at = ?, updated_at = ? "
                        + "WHERE inquiry_id = ? AND status = 'WAITING'",
                request.answer(), Timestamp.from(now), Timestamp.from(now), inquiryId);
        if (updated == 0) {
            Integer exists = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM inquiry WHERE inquiry_id = ?", Integer.class, inquiryId);
            throw new BusinessException(
                    exists != null && exists > 0 ? ErrorCode.INQUIRY_ALREADY_ANSWERED : ErrorCode.INQUIRY_NOT_FOUND);
        }
        if (request.attachmentUrls() != null) {
            int order = 0;
            for (String url : request.attachmentUrls()) {
                answerAttachmentRepository.save(new InquiryAnswerAttachment(inquiryId, url, order++));
            }
        }
        return new InquiryAnswerResponse(inquiryId, "ANSWERED", now);
    }

    private static Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}
