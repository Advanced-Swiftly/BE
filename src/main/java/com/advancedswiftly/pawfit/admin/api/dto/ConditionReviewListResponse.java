package com.advancedswiftly.pawfit.admin.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "입장 조건 심사 목록 (최신순)")
public record ConditionReviewListResponse(List<ConditionReviewListItem> items) {
}
