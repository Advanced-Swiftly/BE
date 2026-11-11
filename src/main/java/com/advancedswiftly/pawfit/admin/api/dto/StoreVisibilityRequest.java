package com.advancedswiftly.pawfit.admin.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "업체 노출 상태 변경 요청")
public record StoreVisibilityRequest(
        @Schema(description = "true면 지도·검색에 노출, false면 숨김", example = "false")
        @NotNull(message = "visible은 필수입니다.")
        Boolean visible,

        @Schema(description = "변경 사유. 현재는 저장하지 않습니다", nullable = true)
        @Size(max = 500, message = "사유는 500자 이하여야 합니다.")
        String reason) {
}
