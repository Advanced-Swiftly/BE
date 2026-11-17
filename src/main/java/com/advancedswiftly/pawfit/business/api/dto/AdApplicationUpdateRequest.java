package com.advancedswiftly.pawfit.business.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;

@Schema(description = "광고 신청 변경. CANCEL 단독 또는 startDate·weeks 중 하나 이상을 전달합니다.")
public record AdApplicationUpdateRequest(
        ApplicationAction action,
        @Schema(description = "변경할 광고 시작일", example = "2026-12-01") LocalDate startDate,
        @Schema(description = "변경할 광고 기간(1~4주)", example = "2") @Min(1) @Max(4) Integer weeks) {

    @AssertTrue(message = "취소와 기간 수정은 함께 요청할 수 없으며, 수정할 항목이 필요합니다.")
    public boolean isChangeValid() {
        return action != null ? startDate == null && weeks == null : startDate != null || weeks != null;
    }

    @AssertTrue(message = "광고 시작일의 연도는 1~9999 범위여야 합니다.")
    public boolean isStartDateValid() {
        return startDate == null || startDate.getYear() >= 1 && startDate.getYear() <= 9999;
    }
}
