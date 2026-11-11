package com.advancedswiftly.pawfit.admin.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "운영자 업체 목록 항목")
public record AdminStoreListItem(
        @Schema(description = "업체 ID", example = "100") Long storeId,
        @Schema(description = "업체명", example = "멍카페") String name,
        @Schema(description = "노출 상태", example = "VISIBLE") String visibility,
        @Schema(description = "지역 ID. 지정 전이면 생략", example = "11", nullable = true) Long regionId,
        @Schema(description = "검증 상태", example = "VERIFIED") String verificationStatus) {
}
