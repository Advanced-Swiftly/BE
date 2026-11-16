package com.advancedswiftly.pawfit.business.api.dto;

import com.advancedswiftly.pawfit.business.domain.AdApplication;
import com.advancedswiftly.pawfit.business.domain.ApplicationStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Schema(description = "프론트 광고 신청 모델과 호환되는 변경 결과")
@JsonInclude(JsonInclude.Include.ALWAYS)
public record AdApplicationUpdateResponse(
        String applicationId, String type, String placeId, String placeName,
        ApplicationStatus status, ApplicationStatus displayStatus, Instant submittedAt,
        String creativeUrl, AdPeriod ad, List<History> history, String rejectReason,
        boolean canCancel, boolean canEditPeriod, boolean canReapply) {

    public static AdApplicationUpdateResponse from(AdApplication application, String storeName) {
        long days = ChronoUnit.DAYS.between(application.getStartDate(), application.getEndDate()) + 1;
        Integer weeks = days % 7 == 0 ? Math.toIntExact(days / 7) : null;
        List<History> history = application.getStatus() == ApplicationStatus.CANCELED
                ? List.of(new History(application.getCreatedAt(), ApplicationStatus.RECEIVED, "BUSINESS"),
                        new History(application.getUpdatedAt(), ApplicationStatus.CANCELED, "BUSINESS"))
                : List.of(new History(application.getCreatedAt(), ApplicationStatus.RECEIVED, "BUSINESS"));
        return new AdApplicationUpdateResponse(application.getId().toString(), "AD",
                application.getStoreId().toString(), storeName, application.getStatus(), application.getStatus(),
                application.getCreatedAt(), application.getCreativeUrl(),
                new AdPeriod(null, application.getStartDate(), weeks, application.getEndDate()),
                history, application.getReviewNote(), application.canCancel(),
                application.getStatus() == ApplicationStatus.RECEIVED, false);
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record AdPeriod(String region, LocalDate startDate, Integer weeks, LocalDate endDate) {
    }

    public record History(Instant at, ApplicationStatus status, String by) {
    }
}
