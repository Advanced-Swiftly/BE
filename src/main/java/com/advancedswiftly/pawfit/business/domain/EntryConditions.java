package com.advancedswiftly.pawfit.business.domain;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/** null인 구역은 기존 공통 규칙을 따릅니다. 구역을 지정하면 구역별 준비물을 전체 지정합니다. */
public record EntryConditions(
        Boolean petsAllowed,
        @Valid Area indoor,
        @Valid Area outdoor,
        LocalDate effectiveFrom,
        @Size(max = 500) String notice,
        @Size(max = 500) String changeReason,
        Boolean vaccinationConfirmed) {
    public record Area(
            Boolean allowed,
            @DecimalMin("0.01") @Digits(integer = 4, fraction = 2) BigDecimal maxWeightKg,
            @Size(max = 20) List<@Pattern(regexp = "[A-Z][A-Z0-9_]{0,49}") String> equipmentCodes) {
        public Area {
            equipmentCodes = equipmentCodes == null ? List.of() : List.copyOf(equipmentCodes);
        }
    }

    public boolean activeOn(LocalDate date) {
        return effectiveFrom == null || !effectiveFrom.isAfter(date);
    }

    public static LocalDate today() {
        return LocalDate.now(ZoneId.of("Asia/Seoul"));
    }
}
