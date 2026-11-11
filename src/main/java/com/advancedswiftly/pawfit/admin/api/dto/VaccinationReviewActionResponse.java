package com.advancedswiftly.pawfit.admin.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "예방접종 검토 처리 결과")
public record VaccinationReviewActionResponse(
        @Schema(description = "기록 ID", example = "20") Long recordId,
        @Schema(description = "처리 후 상태", example = "VERIFIED") String status) {
}
