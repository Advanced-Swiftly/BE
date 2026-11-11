package com.advancedswiftly.pawfit.admin.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "운영자 업체 정보 수정 결과")
public record AdminStoreUpdateResponse(
        @Schema(description = "업체 ID", example = "100") Long storeId,
        @Schema(description = "수정 후 업체명") String name) {
}
