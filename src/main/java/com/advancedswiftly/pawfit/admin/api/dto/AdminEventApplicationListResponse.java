package com.advancedswiftly.pawfit.admin.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "이벤트 참여 신청 심사 목록 (최신순)")
public record AdminEventApplicationListResponse(List<AdminEventApplicationListItem> items) {
}
