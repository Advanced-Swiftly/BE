package com.advancedswiftly.pawfit.admin.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "입장 조건 심사 목록 항목")
public record ConditionReviewListItem(
        @Schema(description = "요청 ID", example = "1301") Long requestId,
        @Schema(description = "업체 ID", example = "100") Long storeId,
        @Schema(description = "요청 종류", example = "CONDITION_REQUEST") String kind,
        @Schema(description = "처리 상태", example = "RECEIVED") String status,
        @Schema(description = "요청 시각 (UTC)") Instant createdAt) {
}
