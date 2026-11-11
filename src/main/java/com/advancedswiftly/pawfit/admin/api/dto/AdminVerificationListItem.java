package com.advancedswiftly.pawfit.admin.api.dto;

import com.advancedswiftly.pawfit.business.domain.VerificationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "사업자 인증 심사 목록 항목")
public record AdminVerificationListItem(
        @Schema(description = "인증 요청 ID", example = "1101") Long verificationId,
        @Schema(description = "업체 ID", example = "100") Long storeId,
        @Schema(description = "상호명", example = "멍카페") String businessName,
        @Schema(description = "사업자등록번호 (숫자 10자리)", example = "1234567890") String businessNumber,
        @Schema(description = "처리 상태", example = "RECEIVED") VerificationStatus status,
        @Schema(description = "요청 시각 (UTC)") Instant createdAt) {
}
