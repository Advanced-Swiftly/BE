package com.advancedswiftly.pawfit.business.api.dto;

import com.advancedswiftly.pawfit.business.domain.ApplicationStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "이벤트 참여조건 사전 확인 결과. baseDate는 한국 날짜 기준입니다.")
@JsonInclude(JsonInclude.Include.ALWAYS)
public record EventEligibilityResponse(
        String eventId,
        String placeId,
        LocalDate baseDate,
        boolean recruitmentOpen,
        ExistingApplication existingApplication,
        List<Check> checks,
        boolean allPassed,
        boolean canApply) {

    public record ExistingApplication(String applicationId, ApplicationStatus status) {
    }

    public record Check(Code code, boolean passed, Detail detail) {
    }

    public enum Code { REGION, BUSINESS_VERIFIED, QR_ACTIVE, CONDITION_CHECKED_RECENT }

    public record Detail(LocalDate checkedDate, Long daysSince, Reason reason) {
    }

    public enum Reason { NO_DATE, FUTURE_DATE, OVER_30_DAYS }
}
