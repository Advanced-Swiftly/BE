package com.advancedswiftly.pawfit.admin.api.dto;

import com.advancedswiftly.pawfit.business.api.dto.RequestedPolicy;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "입장 조건 요청 심사 처리 요청")
public record ConditionReviewRequest(
        @Schema(
                description = "심사 동작. START_REVIEW(접수→심사 중), NEEDS_INFO(심사 중→보완 요청), "
                        + "APPROVE(심사 중→승인), REJECT(심사 중→반려)",
                example = "APPROVE",
                allowableValues = {"START_REVIEW", "NEEDS_INFO", "APPROVE", "REJECT"})
        @NotNull(message = "action은 필수입니다.")
        ApplicationReviewAction action,

        @Schema(description = "보완·반려 사유. NEEDS_INFO, REJECT에서는 필수", nullable = true)
        @Size(max = 500, message = "사유는 500자 이하여야 합니다.")
        String reviewComment,

        @Schema(description = "최종 승인 정책. APPROVE에서 생략하면 요청한 정책을 그대로 적용", nullable = true)
        @Valid
        RequestedPolicy approvedPolicy) {
}
