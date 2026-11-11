package com.advancedswiftly.pawfit.business.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.LocalTime;

@Schema(description = "수정할 요일별 영업시간")
public record BusinessHoursUpdateItem(
        @Schema(description = "요일", example = "MONDAY")
        @NotNull(message = "요일은 필수입니다.")
        DayOfWeek dayOfWeek,

        @Schema(description = "영업 시작 시각 (HH:mm). 영업하는 요일에는 필수", example = "10:00", nullable = true)
        LocalTime openTime,

        @Schema(description = "영업 종료 시각 (HH:mm). 영업하는 요일에는 필수이고 시작보다 늦어야 함", example = "20:00", nullable = true)
        LocalTime closeTime,

        @Schema(description = "휴무 여부. 생략하면 false", example = "false", nullable = true)
        Boolean closed) {
}
