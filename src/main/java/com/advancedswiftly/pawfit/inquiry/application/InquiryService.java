package com.advancedswiftly.pawfit.inquiry.application;

import com.advancedswiftly.pawfit.auth.domain.User;
import com.advancedswiftly.pawfit.auth.infrastructure.UserRepository;
import com.advancedswiftly.pawfit.global.error.BusinessException;
import com.advancedswiftly.pawfit.global.error.ErrorCode;
import com.advancedswiftly.pawfit.inquiry.api.dto.InquiryDtos.InquiryCreateRequest;
import com.advancedswiftly.pawfit.inquiry.api.dto.InquiryDtos.InquiryCreateResponse;
import com.advancedswiftly.pawfit.inquiry.api.dto.InquiryDtos.InquiryDetailResponse;
import com.advancedswiftly.pawfit.inquiry.api.dto.InquiryDtos.InquiryListItem;
import com.advancedswiftly.pawfit.inquiry.api.dto.InquiryDtos.InquiryListResponse;
import com.advancedswiftly.pawfit.inquiry.domain.Inquiry;
import com.advancedswiftly.pawfit.inquiry.domain.InquiryAttachment;
import com.advancedswiftly.pawfit.inquiry.domain.InquiryStatus;
import com.advancedswiftly.pawfit.inquiry.infrastructure.InquiryRepository;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InquiryService {

    private static final int PAGE_SIZE = 20;

    private final InquiryRepository inquiryRepository;
    private final UserRepository userRepository;
    private final org.springframework.jdbc.core.JdbcTemplate db;
    private final com.advancedswiftly.pawfit.global.storage.FileService files;

    public InquiryService(
            InquiryRepository inquiryRepository,
            UserRepository userRepository,
            org.springframework.jdbc.core.JdbcTemplate db,
            com.advancedswiftly.pawfit.global.storage.FileService files) {
        this.files = files;
        this.db = db;
        this.inquiryRepository = inquiryRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public InquiryCreateResponse create(Long userId, InquiryCreateRequest request) {
        User user =
                userRepository
                        .findById(userId)
                        .filter(User::isActive)
                        .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        files.validateReferences(userId, request.attachmentUrls());
        Inquiry inquiry =
                inquiryRepository.save(
                        Inquiry.create(
                                user,
                                request.category(),
                                request.title(),
                                request.content(),
                                request.attachmentUrls()));
        if (request.relatedRequest() != null) {
            String table =
                    switch (request.relatedRequest().type()) {
                        case "AD" -> "ad_application";
                        case "EVENT" -> "event_application";
                        case "CONDITION" -> "condition_request";
                        default -> throw new BusinessException(ErrorCode.INVALID_REQUEST);
                    };
            String key = table.equals("condition_request") ? "request_id" : "application_id";
            Long owned =
                    db.queryForObject(
                            "select count(*) from "
                                    + table
                                    + " where "
                                    + key
                                    + "=? and submitted_by=?",
                            Long.class,
                            request.relatedRequest().id(),
                            userId);
            if (owned == null || owned != 1) throw new BusinessException(ErrorCode.FORBIDDEN);
            inquiry.link(request.relatedRequest().type(), request.relatedRequest().id());
        }
        return new InquiryCreateResponse(inquiry.getId(), inquiry.getStatus());
    }

    @Transactional(readOnly = true)
    public InquiryListResponse findMine(Long userId, InquiryStatus status, int page) {
        Pageable pageable =
                PageRequest.of(page, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<InquiryListItem> items =
                (status == null
                                ? inquiryRepository.findAllByUserId(userId, pageable)
                                : inquiryRepository.findAllByUserIdAndStatus(
                                        userId, status, pageable))
                        .stream()
                                .map(
                                        inquiry ->
                                                new InquiryListItem(
                                                        inquiry.getId(),
                                                        inquiry.getTitle(),
                                                        inquiry.getStatus()))
                                .toList();
        return new InquiryListResponse(items);
    }

    @Transactional(readOnly = true)
    public InquiryDetailResponse findMine(Long userId, Long inquiryId) {
        Inquiry inquiry =
                inquiryRepository
                        .findByIdAndUserId(inquiryId, userId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.INQUIRY_NOT_FOUND));
        return new InquiryDetailResponse(
                inquiry.getId(),
                inquiry.getCategory(),
                inquiry.getTitle(),
                inquiry.getContent(),
                files.resolveReferences(
                        inquiry.getAttachments().stream()
                                .map(InquiryAttachment::getAttachmentUrl)
                                .toList()),
                inquiry.getStatus(),
                inquiry.getAnswer(),
                inquiry.getCreatedAt(),
                inquiry.getAnsweredAt(),
                inquiry.getRelatedId() == null
                        ? null
                        : new com.advancedswiftly.pawfit.inquiry.api.dto.InquiryDtos.RelatedRequest(
                                inquiry.getRelatedType(), inquiry.getRelatedId()));
    }
}
