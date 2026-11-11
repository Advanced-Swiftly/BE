package com.advancedswiftly.pawfit.admin.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "이벤트 추첨 실행 결과")
public record EventDrawResponse(
        @Schema(description = "이벤트 ID", example = "801") Long eventId,
        @Schema(description = "추첨 시각 (UTC)") Instant drawnAt,
        @Schema(description = "당첨자 수", example = "10") int winnerCount) {
}
