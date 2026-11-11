package com.advancedswiftly.pawfit.business.api.dto;

import com.advancedswiftly.pawfit.business.domain.StoreVerification;
import com.advancedswiftly.pawfit.business.domain.VerificationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사업자 인증 상세 조회 결과")
public record VerificationDetailResponse(
        @Schema(description = "인증 요청 ID", example = "1101") Long verificationId,
        @Schema(description = "처리 상태", example = "NEEDS_SUPPLEMENT") VerificationStatus status,
        @Schema(
                description = "심사 의견(보완 요청·승인·반려 사유). 심사 전이면 null",
                example = "등록증 식별이 어렵습니다.",
                nullable = true)
        String reviewComment) {

    public static VerificationDetailResponse from(StoreVerification verification) {
        return new VerificationDetailResponse(
                verification.getId(), verification.getStatus(), verification.getReviewNote());
    }
}
