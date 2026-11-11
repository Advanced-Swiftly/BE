package com.advancedswiftly.pawfit.business.api.dto;

import com.advancedswiftly.pawfit.business.domain.ConditionRequest;
import com.advancedswiftly.pawfit.business.domain.ConditionRequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "입장 조건 변경 요청 접수 결과")
public record ConditionRequestResultResponse(
        @Schema(description = "요청 ID", example = "1301") Long requestId,
        @Schema(description = "처리 상태", example = "RECEIVED") ConditionRequestStatus status) {

    public static ConditionRequestResultResponse from(ConditionRequest request) {
        return new ConditionRequestResultResponse(request.getId(), request.getStatus());
    }
}
