package com.advancedswiftly.pawfit.admin.api.dto;

import com.advancedswiftly.pawfit.business.api.dto.StorePolicyResponse;
import com.advancedswiftly.pawfit.business.domain.ConditionRequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

@Schema(description = "입장 조건 요청 심사 상세")
public record AdminConditionRequestDetailResponse(
        @Schema(description = "요청 ID", example = "1301") Long requestId,
        @Schema(description = "업체 ID", example = "100") Long storeId,
        @Schema(description = "요청 종류", example = "CONDITION_REQUEST") String kind,
        @Schema(description = "처리 상태", example = "IN_REVIEW") ConditionRequestStatus status,
        @Schema(description = "요청의 기준 정책 버전", example = "3") int baseVersion,
        @Schema(description = "정책 원문") String sourceText,
        @Schema(description = "현재 적용 중인 정책. 정책이 없으면 생략", nullable = true) StorePolicyResponse currentPolicy,
        @Schema(description = "요청한 정책") AdminPolicyView requestedPolicy,
        @Schema(description = "현재 정책과의 차이") List<PolicyChangeItem> changes,
        @Schema(description = "근거 파일 URL") List<String> evidenceUrls,
        @Schema(description = "심사 의견. 심사 전이면 생략", nullable = true) String reviewNote,
        @Schema(description = "마지막 심사 시각. 심사 전이면 생략", nullable = true) Instant reviewedAt,
        @Schema(description = "요청 시각 (UTC)") Instant createdAt) {
}
