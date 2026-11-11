package com.advancedswiftly.pawfit.business.api.dto;

import com.advancedswiftly.pawfit.business.domain.AdApplication;
import com.advancedswiftly.pawfit.business.domain.AdType;
import com.advancedswiftly.pawfit.business.domain.ApplicationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "광고 신청 목록의 항목")
public record AdApplicationListItem(
        @Schema(description = "신청 ID", example = "1601") Long applicationId,
        @Schema(description = "광고 종류", example = "BANNER") AdType adType,
        @Schema(description = "처리 상태", example = "IN_REVIEW") ApplicationStatus status) {

    public static AdApplicationListItem from(AdApplication application) {
        return new AdApplicationListItem(
                application.getId(), application.getAdType(), application.getStatus());
    }
}
