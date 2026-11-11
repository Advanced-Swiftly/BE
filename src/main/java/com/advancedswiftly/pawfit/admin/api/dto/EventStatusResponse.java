package com.advancedswiftly.pawfit.admin.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "이벤트 생성·수정 결과")
public record EventStatusResponse(
        @Schema(description = "이벤트 ID", example = "802") Long eventId,
        @Schema(description = "이벤트 상태", example = "DRAFT") String status) {
}
