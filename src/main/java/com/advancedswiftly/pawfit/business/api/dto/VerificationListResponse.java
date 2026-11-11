package com.advancedswiftly.pawfit.business.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "내 사업자 인증 요청 목록")
public record VerificationListResponse(
        @Schema(description = "최신 요청이 먼저 오는 목록") List<VerificationListItem> items) {
}
