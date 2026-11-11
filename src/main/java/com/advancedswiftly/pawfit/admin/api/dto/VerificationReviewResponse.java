package com.advancedswiftly.pawfit.admin.api.dto;

import com.advancedswiftly.pawfit.business.domain.VerificationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사업자 인증 심사 처리 결과")
public record VerificationReviewResponse(
        @Schema(description = "인증 요청 ID", example = "1101") Long verificationId,
        @Schema(description = "처리 후 상태", example = "APPROVED") VerificationStatus status,
        @Schema(description = "업체 ID", example = "100") Long storeId) {
}
