package com.advancedswiftly.pawfit.business.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "사업자 인증 요청")
public record VerificationCreateRequest(
        @Schema(description = "사업자등록번호 (하이픈 허용)", example = "123-45-67890")
        @NotBlank(message = "사업자등록번호는 필수입니다.")
        @Pattern(regexp = "^\\d{3}-?\\d{2}-?\\d{5}$", message = "사업자등록번호 형식이 올바르지 않습니다.")
        String businessNumber,

        @Schema(description = "상호명", example = "성수 댕댕카페")
        @NotBlank(message = "상호명은 필수입니다.")
        @Size(max = 100, message = "상호명은 100자 이하여야 합니다.")
        String businessName,

        @Schema(description = "대표자명", example = "홍길동")
        @NotBlank(message = "대표자명은 필수입니다.")
        @Size(max = 50, message = "대표자명은 50자 이하여야 합니다.")
        String representativeName,

        @Schema(description = "사업자등록증 파일 URL", example = "https://example.com/documents/business-registration.pdf")
        @NotBlank(message = "사업자등록증 파일 URL은 필수입니다.")
        @Size(max = 500, message = "파일 URL은 500자 이하여야 합니다.")
        @Pattern(regexp = "^https?://.+", message = "파일 URL 형식이 올바르지 않습니다.")
        String documentUrl,

        @NotNull(message = "업체 정보는 필수입니다.")
        @Valid
        StoreRequest store) {
}
