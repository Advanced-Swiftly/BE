package com.advancedswiftly.pawfit.business.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "사업자 인증 보완 제출 요청")
public record VerificationResubmitRequest(
        @Schema(description = "새 사업자등록증 URL", example = "https://example.com/business-license-v2.pdf")
        @NotBlank(message = "사업자등록증 URL은 필수입니다.")
        @Size(max = 500, message = "사업자등록증 URL은 500자 이하여야 합니다.")
        @Pattern(regexp = "^https?://.+", message = "사업자등록증 URL 형식이 올바르지 않습니다.")
        String documentUrl,

        @Schema(description = "수정할 업체 정보 (선택)", nullable = true)
        @Valid
        StoreUpdateRequest store) {
}
