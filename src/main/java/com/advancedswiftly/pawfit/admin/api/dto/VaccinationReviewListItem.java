package com.advancedswiftly.pawfit.admin.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "예방접종 검토 목록 항목")
public record VaccinationReviewListItem(
        @Schema(description = "기록 ID", example = "20") Long recordId,
        @Schema(description = "반려견 ID", example = "5") Long dogId,
        @Schema(description = "반려견 이름", example = "콩이") String dogName,
        @Schema(description = "검토 상태", example = "OPERATOR_REVIEW") String status) {
}
