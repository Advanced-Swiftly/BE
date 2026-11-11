package com.advancedswiftly.pawfit.admin.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "정책 변경 이력 (버전 내림차순)")
public record PolicyHistoryResponse(List<PolicyHistoryItem> items) {
}
