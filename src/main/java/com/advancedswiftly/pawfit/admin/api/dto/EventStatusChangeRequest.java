package com.advancedswiftly.pawfit.admin.api.dto;

import com.advancedswiftly.pawfit.event.domain.EventStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "이벤트 상태 변경 요청")
public record EventStatusChangeRequest(
        @Schema(
                description = "바꿀 상태. DRAFT→OPEN(공개), OPEN→CLOSED(종료), CLOSED→DRAWN(추첨 완료, 추첨 실행 후에만)",
                example = "CLOSED",
                allowableValues = {"OPEN", "CLOSED", "DRAWN"})
        @NotNull(message = "status는 필수입니다.")
        EventStatus status) {
}
