package com.advancedswiftly.pawfit.business.api.dto;

import com.advancedswiftly.pawfit.business.domain.AdApplication;
import com.advancedswiftly.pawfit.business.domain.ApplicationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "광고 신청 상세")
public record AdApplicationDetailResponse(
        @Schema(description = "신청 ID", example = "1601") Long applicationId,
        @Schema(description = "처리 상태", example = "IN_REVIEW") ApplicationStatus status,
        @Schema(description = "광고 소재 URL", example = "https://cdn.pawfit.app/ad/1601.png")
        String creativeUrl) {

    public static AdApplicationDetailResponse from(AdApplication application) {
        return new AdApplicationDetailResponse(
                application.getId(), application.getStatus(), application.getCreativeUrl());
    }
}
