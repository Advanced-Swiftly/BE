package com.advancedswiftly.pawfit.business.api.dto;

import com.advancedswiftly.pawfit.business.domain.AdType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

@Schema(description = "광고 신청 요청")
public record AdApplicationCreateRequest(
        @Schema(description = "신청 업체 ID", example = "11")
        @NotNull(message = "업체 ID는 필수입니다.")
        Long storeId,

        @Schema(description = "광고 종류", example = "BANNER", allowableValues = {"BANNER", "RECOMMENDED"})
        @NotNull(message = "광고 종류는 필수입니다.")
        AdType adType,

        @Schema(description = "희망 시작일", example = "2026-12-01")
        @NotNull(message = "시작일은 필수입니다.")
        LocalDate startDate,

        @Schema(description = "희망 종료일", example = "2026-12-31")
        @NotNull(message = "종료일은 필수입니다.")
        LocalDate endDate,

        @Schema(description = "광고 소재 URL", example = "https://cdn.pawfit.app/ad/1601.png")
        @NotBlank(message = "광고 소재 URL은 필수입니다.")
        @Size(max = 500, message = "광고 소재 URL은 500자 이하여야 합니다.")
        @Pattern(regexp = "^https?://.+", message = "광고 소재 URL 형식이 올바르지 않습니다.")
        String creativeUrl,

        @Schema(description = "운영자 전달사항", example = "연말 프로모션용입니다.", nullable = true)
        @Size(max = 500, message = "전달사항은 500자 이하여야 합니다.")
        String message) {
}
