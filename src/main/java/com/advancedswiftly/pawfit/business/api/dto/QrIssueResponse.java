package com.advancedswiftly.pawfit.business.api.dto;

import com.advancedswiftly.pawfit.business.domain.QrStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "업체 QR 발급 결과")
public record QrIssueResponse(
        @Schema(description = "새 QR ID", example = "1402") Long qrId,
        @Schema(description = "QR 상태", example = "ACTIVE") QrStatus status,
        @Schema(description = "기존 QR을 폐기했는지 여부", example = "true") boolean revokedPreviousQr) {
}
