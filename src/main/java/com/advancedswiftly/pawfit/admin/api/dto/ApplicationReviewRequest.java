package com.advancedswiftly.pawfit.admin.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "신청 심사 처리 요청")
public record ApplicationReviewRequest(
        @Schema(
                description = "심사 동작. START_REVIEW(접수→심사 중), NEEDS_INFO(심사 중→보완 요청), "
                        + "APPROVE(심사 중→승인), REJECT(심사 중→반려)",
                example = "APPROVE",
                allowableValues = {"START_REVIEW", "NEEDS_INFO", "APPROVE", "REJECT"})
        @NotNull(message = "action은 필수입니다.")
        ApplicationReviewAction action,

        @Schema(description = "보완·반려 사유. NEEDS_INFO, REJECT에서는 필수", example = "소재 해상도가 낮습니다.", nullable = true)
        @Size(max = 500, message = "사유는 500자 이하여야 합니다.")
        String comment) {
}
