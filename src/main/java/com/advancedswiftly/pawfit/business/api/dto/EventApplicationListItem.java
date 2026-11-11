package com.advancedswiftly.pawfit.business.api.dto;

import com.advancedswiftly.pawfit.business.domain.ApplicationStatus;
import com.advancedswiftly.pawfit.business.domain.EventApplication;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "이벤트 신청 목록의 항목")
public record EventApplicationListItem(
        @Schema(description = "신청 ID", example = "1501") Long applicationId,
        @Schema(description = "이벤트 ID", example = "801") Long eventId,
        @Schema(description = "처리 상태", example = "APPROVED") ApplicationStatus status) {

    public static EventApplicationListItem from(EventApplication application) {
        return new EventApplicationListItem(
                application.getId(), application.getEventId(), application.getStatus());
    }
}
