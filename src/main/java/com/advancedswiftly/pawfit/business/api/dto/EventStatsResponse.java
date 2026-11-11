package com.advancedswiftly.pawfit.business.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "업체 이벤트 통계")
public record EventStatsResponse(
        @Schema(description = "업체 ID", example = "100") Long storeId,
        @Schema(description = "이벤트 ID", example = "801") Long eventId,
        @Schema(description = "이벤트 기간 내 방문 수", example = "120") long visits,
        @Schema(description = "이 업체에서 지급한 스탬프 수", example = "85") long stampAwards,
        @Schema(description = "이 업체에서 스탬프를 받은 뒤 응모한 사용자 수", example = "12") long drawConversions) {
}
