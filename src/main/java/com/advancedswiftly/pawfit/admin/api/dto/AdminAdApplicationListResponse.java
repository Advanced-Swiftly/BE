package com.advancedswiftly.pawfit.admin.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "광고 신청 심사 목록 (최신순)")
public record AdminAdApplicationListResponse(List<AdminAdApplicationListItem> items) {
}
