package com.advancedswiftly.pawfit.admin.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "사업자 인증 심사 목록 (최신순)")
public record AdminVerificationListResponse(List<AdminVerificationListItem> items) {
}
