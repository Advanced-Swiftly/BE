package com.advancedswiftly.pawfit.business.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "신청 취소 요청")
public record ApplicationCancelRequest(
        @Schema(description = "수행할 동작", example = "CANCEL", allowableValues = {"CANCEL"})
        @NotNull(message = "action은 필수입니다.")
        ApplicationAction action) {
}
