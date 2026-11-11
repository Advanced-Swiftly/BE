package com.advancedswiftly.pawfit.business.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "영업시간 수정 결과")
public record BusinessHoursUpdateResponse(
        @Schema(description = "업체 ID", example = "100") Long storeId,
        @Schema(description = "변경한 요일 수", example = "7") int updatedDays) {
}
