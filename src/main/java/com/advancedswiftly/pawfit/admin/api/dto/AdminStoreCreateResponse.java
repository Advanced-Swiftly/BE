package com.advancedswiftly.pawfit.admin.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "운영자 업체 직접 등록 결과")
public record AdminStoreCreateResponse(
        @Schema(description = "업체 ID", example = "101") Long storeId,
        @Schema(description = "노출 상태. 등록 직후에는 HIDDEN", example = "HIDDEN") String visibility) {
}
