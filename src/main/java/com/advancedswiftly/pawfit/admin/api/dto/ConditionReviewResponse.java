package com.advancedswiftly.pawfit.admin.api.dto;

import com.advancedswiftly.pawfit.business.domain.ConditionRequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "입장 조건 요청 심사 처리 결과")
public record ConditionReviewResponse(
        @Schema(description = "요청 ID", example = "1301") Long requestId,
        @Schema(description = "처리 후 상태", example = "APPROVED") ConditionRequestStatus status,
        @Schema(description = "새로 적용된 정책 버전. 승인일 때만 있음", example = "4", nullable = true) Integer appliedPolicyVersion) {
}
