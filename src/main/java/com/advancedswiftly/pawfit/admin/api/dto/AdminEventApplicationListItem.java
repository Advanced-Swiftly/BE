package com.advancedswiftly.pawfit.admin.api.dto;

import com.advancedswiftly.pawfit.business.domain.ApplicationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

@Schema(description = "이벤트 참여 신청 심사 목록 항목")
public record AdminEventApplicationListItem(
        @Schema(description = "신청 ID", example = "1501") Long applicationId,
        @Schema(description = "업체 ID", example = "100") Long storeId,
        @Schema(description = "업체명. 업체가 없으면 생략", nullable = true) String storeName,
        @Schema(description = "이벤트 ID", example = "801") Long eventId,
        @Schema(description = "신청 지역 ID") List<Long> regionIds,
        @Schema(description = "처리 상태", example = "RECEIVED") ApplicationStatus status,
        @Schema(description = "신청 시각 (UTC)") Instant createdAt) {
}
