package com.advancedswiftly.pawfit.admin.api.dto;

import com.advancedswiftly.pawfit.business.domain.AdType;
import com.advancedswiftly.pawfit.business.domain.ApplicationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;

@Schema(description = "광고 신청 심사 목록 항목")
public record AdminAdApplicationListItem(
        @Schema(description = "신청 ID", example = "1601") Long applicationId,
        @Schema(description = "업체 ID", example = "100") Long storeId,
        @Schema(description = "업체명. 업체가 없으면 생략", nullable = true) String storeName,
        @Schema(description = "광고 유형", example = "BANNER") AdType adType,
        @Schema(description = "노출 시작일") LocalDate startDate,
        @Schema(description = "노출 종료일") LocalDate endDate,
        @Schema(description = "처리 상태", example = "RECEIVED") ApplicationStatus status,
        @Schema(description = "신청 시각 (UTC)") Instant createdAt) {
}
