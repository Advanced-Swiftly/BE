package com.advancedswiftly.pawfit.admin.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "추첨 응모자 항목")
public record DrawEntryListItem(
        @Schema(description = "응모 ID", example = "1001") Long drawEntryId,
        @Schema(description = "사용자 ID", example = "1") Long userId,
        @Schema(description = "응모 상태", example = "ENTERED") String status,
        @Schema(description = "보유 스탬프 수", example = "10") long stampCount,
        @Schema(description = "연락·배송 상태. 당첨 전이면 생략", example = "SHIPPED", nullable = true) String fulfillmentStatus) {
}
