package com.advancedswiftly.pawfit.admin.api.dto;

import com.advancedswiftly.pawfit.business.domain.ApplicationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "신청 심사 처리 결과")
public record ApplicationReviewResponse(
        @Schema(description = "신청 ID", example = "1601") Long applicationId,
        @Schema(description = "처리 후 상태", example = "APPROVED") ApplicationStatus status) {
}
