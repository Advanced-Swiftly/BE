package com.advancedswiftly.pawfit.business.api.dto;

import com.advancedswiftly.pawfit.business.domain.ConditionRequest;
import com.advancedswiftly.pawfit.business.domain.ConditionRequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "입장 조건 요청 목록의 항목")
public record ConditionRequestListItem(
        @Schema(description = "요청 ID", example = "1301") Long requestId,
        @Schema(description = "요청 종류. 현재는 CONDITION_REQUEST만 있음", example = "CONDITION_REQUEST") String kind,
        @Schema(description = "처리 상태", example = "IN_REVIEW") ConditionRequestStatus status) {

    public static final String KIND_CONDITION_REQUEST = "CONDITION_REQUEST";

    public static ConditionRequestListItem from(ConditionRequest request) {
        return new ConditionRequestListItem(request.getId(), KIND_CONDITION_REQUEST, request.getStatus());
    }
}
