package com.advancedswiftly.pawfit.business.api.dto;

import com.advancedswiftly.pawfit.business.domain.ConditionRequest;
import com.advancedswiftly.pawfit.business.domain.ConditionRequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "입장 조건 변경 요청 상세")
public record ConditionRequestDetailResponse(
        @Schema(description = "요청 ID", example = "1301") Long requestId,
        @Schema(description = "처리 상태", example = "NEEDS_SUPPLEMENT") ConditionRequestStatus status,
        @Schema(
                description = "심사 의견(보완 요청·반려 사유). 심사 전이면 필드가 응답에서 빠집니다",
                example = "예방접종 기준을 명시해 주세요.",
                nullable = true)
        String reviewComment) {

    public static ConditionRequestDetailResponse from(ConditionRequest request) {
        return new ConditionRequestDetailResponse(request.getId(), request.getStatus(), request.getReviewNote());
    }
}
