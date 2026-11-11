package com.advancedswiftly.pawfit.admin.api.dto;

import com.advancedswiftly.pawfit.business.domain.PolicyRuleType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "현재 정책과 요청 정책의 차이 하나")
public record PolicyChangeItem(
        @Schema(description = "조건 종류", example = "EQUIPMENT") PolicyRuleType ruleType,
        @Schema(description = "조건 코드", example = "CARRIER") String code,
        @Schema(description = "변화. ADDED(추가), REMOVED(삭제), REQUIRED_CHANGED(필수 여부 변경)", example = "ADDED") String change,
        @Schema(description = "변경 전 필수 여부. 추가된 조건이면 생략", nullable = true) Boolean beforeRequired,
        @Schema(description = "변경 후 필수 여부. 삭제된 조건이면 생략", nullable = true) Boolean afterRequired) {
}
