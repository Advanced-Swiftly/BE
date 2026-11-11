package com.advancedswiftly.pawfit.business.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

@Schema(description = "기간별 방문 수")
public record VisitStatsPoint(
        @Schema(description = "구간 시작일 (DAY는 그날, WEEK는 월요일, MONTH는 1일)", example = "2026-11-01") LocalDate date,
        @Schema(description = "방문 수", example = "12") long visits) {
}
