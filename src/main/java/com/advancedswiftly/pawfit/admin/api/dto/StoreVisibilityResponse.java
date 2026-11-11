package com.advancedswiftly.pawfit.admin.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "업체 노출 상태 변경 결과")
public record StoreVisibilityResponse(
        @Schema(description = "업체 ID", example = "100") Long storeId,
        @Schema(description = "변경 후 노출 상태", example = "VISIBLE") String visibility) {
}
