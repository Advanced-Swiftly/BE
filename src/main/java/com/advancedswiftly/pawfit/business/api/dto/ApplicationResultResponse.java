package com.advancedswiftly.pawfit.business.api.dto;

import com.advancedswiftly.pawfit.business.domain.ApplicationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "신청 처리 결과")
public record ApplicationResultResponse(
        @Schema(description = "신청 ID", example = "1501") Long applicationId,
        @Schema(description = "처리 상태", example = "RECEIVED") ApplicationStatus status) {

    public static ApplicationResultResponse of(Long applicationId, ApplicationStatus status) {
        return new ApplicationResultResponse(applicationId, status);
    }
}
