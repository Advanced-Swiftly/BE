package com.advancedswiftly.pawfit.business.api.dto;

import com.advancedswiftly.pawfit.business.domain.StoreVerification;
import com.advancedswiftly.pawfit.business.domain.VerificationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사업자 인증 요청 결과")
public record VerificationCreateResponse(
        @Schema(description = "인증 요청 ID", example = "1101") Long verificationId,
        @Schema(description = "처리 상태", example = "RECEIVED") VerificationStatus status) {

    public static VerificationCreateResponse from(StoreVerification verification) {
        return new VerificationCreateResponse(verification.getId(), verification.getStatus());
    }
}
