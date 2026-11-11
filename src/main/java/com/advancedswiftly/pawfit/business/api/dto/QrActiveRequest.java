package com.advancedswiftly.pawfit.business.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "업체 QR 활성 상태 변경 요청")
public record QrActiveRequest(
        @Schema(description = "활성 여부", example = "false")
        @NotNull(message = "활성 여부는 필수입니다.")
        Boolean active) {
}
