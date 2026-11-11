package com.advancedswiftly.pawfit.admin.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

@Schema(description = "이벤트 목록 항목")
public record AdminEventListItem(
        @Schema(description = "이벤트 ID", example = "801") Long eventId,
        @Schema(description = "이벤트명") String title,
        @Schema(description = "연도", example = "2026") int year,
        @Schema(description = "분기", example = "4") int quarter,
        @Schema(description = "상태", example = "OPEN") String status,
        @Schema(description = "시작일", example = "2026-10-01") LocalDate startsOn,
        @Schema(description = "종료일", example = "2026-12-31") LocalDate endsOn) {
}
