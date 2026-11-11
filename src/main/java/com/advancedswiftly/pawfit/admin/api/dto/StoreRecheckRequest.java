package com.advancedswiftly.pawfit.admin.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "업체 조건 재검토 요청 생성 요청")
public record StoreRecheckRequest(
        @Schema(description = "재검토 사유", example = "현장 방문 결과 정책과 다름")
        @NotBlank(message = "재검토 사유는 필수입니다.")
        @Size(max = 2000, message = "재검토 사유는 2000자 이하여야 합니다.")
        String reason) {
}
