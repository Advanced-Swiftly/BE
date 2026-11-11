package com.advancedswiftly.pawfit.admin.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "이벤트 추첨 실행 요청")
public record EventDrawRequest(
        @Schema(description = "실행 확인. true여야 추첨이 실행됩니다", example = "true")
        @NotNull(message = "confirm은 필수입니다.")
        Boolean confirm,

        @Schema(description = "재현 가능한 추첨 시드. 생략하면 서버가 만들어 기록합니다", nullable = true)
        @Size(max = 100, message = "시드는 100자 이하여야 합니다.")
        String seed) {
}
