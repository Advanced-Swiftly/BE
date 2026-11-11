package com.advancedswiftly.pawfit.admin.api.dto;

import com.advancedswiftly.pawfit.business.domain.ConditionRequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "업체 조건 재검토 요청 생성 결과")
public record StoreRecheckResponse(
        @Schema(description = "요청 ID", example = "1302") Long requestId,
        @Schema(description = "요청 종류", example = "RECHECK") String kind,
        @Schema(description = "처리 상태", example = "RECEIVED") ConditionRequestStatus status) {
}
