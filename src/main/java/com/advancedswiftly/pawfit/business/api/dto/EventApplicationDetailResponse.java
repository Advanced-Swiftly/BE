package com.advancedswiftly.pawfit.business.api.dto;

import com.advancedswiftly.pawfit.business.domain.ApplicationStatus;
import com.advancedswiftly.pawfit.business.domain.EventApplication;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "이벤트 신청 상세")
public record EventApplicationDetailResponse(
        @Schema(description = "신청 ID", example = "1501") Long applicationId,
        @Schema(description = "처리 상태", example = "APPROVED") ApplicationStatus status,
        @Schema(
                description = "심사 의견. 심사 전이면 필드가 응답에서 빠집니다",
                example = "참여가 승인되었습니다.",
                nullable = true)
        String reviewComment) {

    public static EventApplicationDetailResponse from(EventApplication application) {
        return new EventApplicationDetailResponse(
                application.getId(), application.getStatus(), application.getReviewNote());
    }
}
