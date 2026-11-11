package com.advancedswiftly.pawfit.admin.api.dto;

import com.advancedswiftly.pawfit.business.domain.ConditionRequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "운영자 정책 직접 변경 결과")
public record PolicyChangeResponse(
        @Schema(description = "기록용 요청 ID", example = "1303") Long requestId,
        @Schema(description = "요청 종류", example = "OPERATOR_DIRECT") String kind,
        @Schema(description = "처리 상태", example = "APPROVED") ConditionRequestStatus status,
        @Schema(description = "새로 적용된 정책 버전", example = "4") int appliedPolicyVersion) {
}
