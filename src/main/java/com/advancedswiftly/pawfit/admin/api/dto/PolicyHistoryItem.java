package com.advancedswiftly.pawfit.admin.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "정책 변경 이력 항목")
public record PolicyHistoryItem(
        @Schema(description = "정책 ID", example = "1201") Long policyId,
        @Schema(description = "정책 버전", example = "4") int version,
        @Schema(description = "변경 주체. 이력이 기록되기 전 정책은 생략", example = "OPERATOR", nullable = true) String changedBy,
        @Schema(description = "승인된 조건 변경 요청 ID. 없으면 생략", example = "1301", nullable = true) Long requestId,
        @Schema(description = "정책 원문. 없으면 생략", nullable = true) String sourceText,
        @Schema(description = "적용된 시각 (UTC)") Instant createdAt) {
}
