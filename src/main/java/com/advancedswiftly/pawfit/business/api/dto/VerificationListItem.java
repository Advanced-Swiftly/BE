package com.advancedswiftly.pawfit.business.api.dto;

import com.advancedswiftly.pawfit.business.domain.StoreVerification;
import com.advancedswiftly.pawfit.business.domain.VerificationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사업자 인증 요청 목록의 항목")
public record VerificationListItem(
        @Schema(description = "인증 요청 ID", example = "1101") Long verificationId,
        @Schema(description = "처리 상태", example = "IN_REVIEW") VerificationStatus status,
        @Schema(description = "상호명", example = "멍카페") String businessName) {

    public static VerificationListItem from(StoreVerification verification) {
        return new VerificationListItem(
                verification.getId(), verification.getStatus(), verification.getBusinessName());
    }
}
