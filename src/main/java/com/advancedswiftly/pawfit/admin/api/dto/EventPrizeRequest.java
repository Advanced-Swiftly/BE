package com.advancedswiftly.pawfit.admin.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "이벤트 경품")
public record EventPrizeRequest(
        @Schema(description = "순위", example = "1")
        @NotNull(message = "경품 순위는 필수입니다.")
        @Min(value = 1, message = "경품 순위는 1 이상이어야 합니다.")
        Integer rank,

        @Schema(description = "경품명", example = "펫 유모차")
        @NotBlank(message = "경품명은 필수입니다.")
        @Size(max = 150, message = "경품명은 150자 이하여야 합니다.")
        String name,

        @Schema(description = "수량", example = "3")
        @NotNull(message = "경품 수량은 필수입니다.")
        @Min(value = 1, message = "경품 수량은 1 이상이어야 합니다.")
        Integer quantity) {
}
