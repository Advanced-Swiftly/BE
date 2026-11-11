package com.advancedswiftly.pawfit.admin.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "당첨·배송 상태 변경 결과")
public record DrawEntryActionResponse(
        @Schema(description = "응모 ID", example = "1001") Long drawEntryId,
        @Schema(description = "응모 상태", example = "WON") String status,
        @Schema(description = "연락·배송 상태. 없으면 생략", example = "SHIPPED", nullable = true) String fulfillmentStatus) {
}
