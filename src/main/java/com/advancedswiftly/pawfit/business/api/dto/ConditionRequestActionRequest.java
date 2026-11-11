package com.advancedswiftly.pawfit.business.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "입장 조건 요청 취소·보완 요청. 보완 항목은 RESUBMIT일 때만 사용하며, 생략한 항목은 기존 값을 유지한다")
public record ConditionRequestActionRequest(
        @Schema(description = "수행할 동작", example = "RESUBMIT", allowableValues = {"CANCEL", "RESUBMIT"})
        @NotNull(message = "action은 필수입니다.")
        ConditionRequestAction action,

        @Schema(description = "보완된 정책. 전달하면 기존 변경안 전체를 교체", nullable = true)
        @Valid
        RequestedPolicy requestedPolicy,

        @Schema(description = "보완된 정책 원문", nullable = true)
        @Pattern(regexp = ".*\\S.*", message = "정책 원문은 공백일 수 없습니다.")
        @Size(max = 2000, message = "정책 원문은 2000자 이하여야 합니다.")
        String sourceText,

        @Schema(description = "추가 근거 파일 URL 목록 (기존 근거에 추가)", nullable = true)
        @Size(max = 10, message = "근거 파일은 10개 이하여야 합니다.")
        List<@NotBlank(message = "근거 파일 URL은 비어 있을 수 없습니다.")
                @Size(max = 500, message = "근거 파일 URL은 500자 이하여야 합니다.")
                @Pattern(regexp = "^https?://.+", message = "근거 파일 URL 형식이 올바르지 않습니다.") String> evidenceUrls) {
}
