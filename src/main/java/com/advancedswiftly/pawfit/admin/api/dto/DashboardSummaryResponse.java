package com.advancedswiftly.pawfit.admin.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "운영자 대시보드 요약")
public record DashboardSummaryResponse(
        @Schema(description = "심사 대기 중인 사업자 인증 수 (접수·심사 중)", example = "4") long pendingBusinessVerifications,
        @Schema(description = "심사 대기 중인 입장 조건 변경 요청 수 (접수·심사 중)", example = "7") long pendingConditionRequests,
        @Schema(description = "인증이 완료된 업체 수", example = "35") long activeStores,
        @Schema(description = "방문 인증 수. from/to를 주면 그 기간만 집계", example = "1250") long totalVisits) {
}
