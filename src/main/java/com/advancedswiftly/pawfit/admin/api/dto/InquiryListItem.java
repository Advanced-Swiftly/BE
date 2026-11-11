package com.advancedswiftly.pawfit.admin.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "문의 목록 항목")
public record InquiryListItem(
        @Schema(description = "문의 ID", example = "701") Long inquiryId,
        @Schema(description = "문의 카테고리", example = "EVENT") String category,
        @Schema(description = "제목", example = "이벤트 문의") String title,
        @Schema(description = "답변 상태", example = "WAITING") String status,
        @Schema(description = "등록 시각 (UTC)") Instant createdAt) {
}
