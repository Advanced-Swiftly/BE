package com.advancedswiftly.pawfit.business.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "입장 조건 요청 목록")
public record ConditionRequestListResponse(List<ConditionRequestListItem> items) {
}
