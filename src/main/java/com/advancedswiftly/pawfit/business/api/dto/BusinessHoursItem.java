package com.advancedswiftly.pawfit.business.api.dto;

import com.advancedswiftly.pawfit.business.domain.StoreBusinessHours;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.DayOfWeek;
import java.time.format.DateTimeFormatter;

@Schema(description = "요일별 영업시간")
public record BusinessHoursItem(
        @Schema(description = "요일", example = "MONDAY") DayOfWeek dayOfWeek,
        @Schema(description = "영업 시작 시각 (HH:mm). 휴무일이면 생략", example = "10:00", nullable = true) String openTime,
        @Schema(description = "영업 종료 시각 (HH:mm). 휴무일이면 생략", example = "20:00", nullable = true) String closeTime,
        @Schema(description = "휴무 여부", example = "false") boolean closed) {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    public static BusinessHoursItem from(StoreBusinessHours hours) {
        return new BusinessHoursItem(
                hours.getDayOfWeek(),
                hours.getOpenTime() == null ? null : hours.getOpenTime().format(TIME),
                hours.getCloseTime() == null ? null : hours.getCloseTime().format(TIME),
                hours.isClosed());
    }
}
