package com.advancedswiftly.pawfit.business.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "업체 QR 발급·재발급 요청")
public record QrIssueRequest(
        @Schema(description = "재발급 사유", example = "QR 코드 분실", nullable = true)
        @Size(max = 200, message = "재발급 사유는 200자 이하여야 합니다.")
        String reason) {
}
