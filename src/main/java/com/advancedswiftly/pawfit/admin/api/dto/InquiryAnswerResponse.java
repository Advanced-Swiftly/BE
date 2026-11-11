package com.advancedswiftly.pawfit.admin.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "문의 답변 등록 결과")
public record InquiryAnswerResponse(
        @Schema(description = "문의 ID", example = "701") Long inquiryId,
        @Schema(description = "답변 상태", example = "ANSWERED") String status,
        @Schema(description = "답변 시각 (UTC)") Instant answeredAt) {
}
