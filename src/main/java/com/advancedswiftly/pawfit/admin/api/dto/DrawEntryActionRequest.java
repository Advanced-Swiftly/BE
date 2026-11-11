package com.advancedswiftly.pawfit.admin.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "당첨·배송 상태 변경 요청")
public record DrawEntryActionRequest(
        @Schema(
                description = "동작. CANCEL_WIN(당첨 취소), CONTACTED(연락 완료), ADDRESS_CONFIRMED(주소 확인), "
                        + "SHIPPED(발송, 운송장 필수), DELIVERED(배송 완료)",
                example = "SHIPPED",
                allowableValues = {"CANCEL_WIN", "CONTACTED", "ADDRESS_CONFIRMED", "SHIPPED", "DELIVERED"})
        @NotNull(message = "action은 필수입니다.")
        DrawEntryAction action,

        @Schema(description = "처리 메모", nullable = true)
        @Size(max = 500, message = "메모는 500자 이하여야 합니다.")
        String note,

        @Schema(description = "운송장 번호. SHIPPED에서는 필수", example = "1234567890", nullable = true)
        @Size(max = 50, message = "운송장 번호는 50자 이하여야 합니다.")
        String trackingNumber) {
}
