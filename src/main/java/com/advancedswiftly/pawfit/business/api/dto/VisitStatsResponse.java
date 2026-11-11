package com.advancedswiftly.pawfit.business.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "업체 방문 인증 통계")
public record VisitStatsResponse(
        @Schema(description = "기간 내 QR 인증 방문 수", example = "120") long totalVisits,
        @Schema(description = "기간 내 스탬프 지급 수", example = "85") long stampAwards,
        @Schema(description = "구간별 방문 수 (방문이 있는 구간만, 날짜 오름차순)") List<VisitStatsPoint> series) {
}
