package com.advancedswiftly.pawfit.admin.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "문의자")
public record InquiryUser(
        @Schema(description = "사용자 ID", example = "1") Long userId,
        @Schema(description = "이름", example = "홍길동") String name) {
}
