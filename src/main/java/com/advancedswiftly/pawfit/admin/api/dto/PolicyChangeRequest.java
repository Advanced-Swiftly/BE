package com.advancedswiftly.pawfit.admin.api.dto;

import com.advancedswiftly.pawfit.business.api.dto.RequestedPolicy;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "운영자 정책 직접 변경 요청")
public record PolicyChangeRequest(
        @Schema(description = "기준 정책 버전. 정책이 없으면 0", example = "3")
        @NotNull(message = "기준 버전은 필수입니다.")
        @Min(value = 0, message = "기준 버전은 0 이상이어야 합니다.")
        Integer baseVersion,

        @Schema(description = "적용할 구조화 정책")
        @NotNull(message = "정책은 필수입니다.")
        @Valid
        RequestedPolicy policy,

        @Schema(description = "정책 원문", example = "소형견만 입장 가능하며 이동장이 필요합니다.")
        @NotBlank(message = "정책 원문은 필수입니다.")
        @Size(max = 2000, message = "정책 원문은 2000자 이하여야 합니다.")
        String sourceText,

        @Schema(description = "직접 변경 사유", example = "현장 확인 결과 반영")
        @NotBlank(message = "변경 사유는 필수입니다.")
        @Size(max = 500, message = "변경 사유는 500자 이하여야 합니다.")
        String reason) {
}
