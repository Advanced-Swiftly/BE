package com.advancedswiftly.pawfit.business.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "입장 조건 변경 요청")
public record ConditionRequestCreateRequest(
        @Schema(description = "변경 기준 정책 버전 (정책이 없으면 0)", example = "3")
        @NotNull(message = "기준 정책 버전은 필수입니다.")
        @Min(value = 0, message = "기준 정책 버전은 0 이상이어야 합니다.")
        Integer baseVersion,

        @Schema(description = "변경할 구조화 정책")
        @NotNull(message = "변경할 정책은 필수입니다.")
        @Valid
        RequestedPolicy requestedPolicy,

        @Schema(description = "정책 원문", example = "소형견만 입장 가능하며 이동장이 필요합니다.")
        @NotBlank(message = "정책 원문은 필수입니다.")
        @Size(max = 2000, message = "정책 원문은 2000자 이하여야 합니다.")
        String sourceText,

        @Schema(description = "근거 파일 URL 목록", example = "[\"https://example.com/evidence.png\"]", nullable = true)
        @Size(max = 10, message = "근거 파일은 10개 이하여야 합니다.")
        List<@NotBlank(message = "근거 파일 URL은 비어 있을 수 없습니다.")
                @Size(max = 500, message = "근거 파일 URL은 500자 이하여야 합니다.")
                @Pattern(regexp = "^https?://.+", message = "근거 파일 URL 형식이 올바르지 않습니다.") String> evidenceUrls) {
}
