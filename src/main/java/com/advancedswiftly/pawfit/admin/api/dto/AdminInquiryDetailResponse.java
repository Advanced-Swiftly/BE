package com.advancedswiftly.pawfit.admin.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

@Schema(description = "운영자용 문의 상세")
public record AdminInquiryDetailResponse(
        @Schema(description = "문의 ID", example = "701") Long inquiryId,
        InquiryUser user,
        @Schema(description = "문의 카테고리", example = "EVENT") String category,
        @Schema(description = "제목") String title,
        @Schema(description = "문의 본문") String content,
        @Schema(description = "답변 상태", example = "WAITING") String status,
        @Schema(description = "문의 첨부파일 URL") List<String> attachmentUrls,
        @Schema(description = "등록 시각 (UTC)") Instant createdAt,
        @Schema(description = "답변 내용. 답변 전이면 필드가 응답에서 빠집니다", nullable = true) String answer,
        @Schema(description = "답변 시각. 답변 전이면 생략", nullable = true) Instant answeredAt,
        @Schema(description = "답변 첨부파일 URL") List<String> answerAttachmentUrls) {
}
