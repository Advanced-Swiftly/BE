package com.advancedswiftly.pawfit.admin.api.dto;

import com.advancedswiftly.pawfit.business.domain.AdApplication;
import com.advancedswiftly.pawfit.business.domain.AdType;
import com.advancedswiftly.pawfit.business.domain.ApplicationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;

@Schema(description = "광고 신청 심사 상세")
public record AdminAdApplicationDetailResponse(
        @Schema(description = "신청 ID", example = "1601") Long applicationId,
        @Schema(description = "업체 ID", example = "100") Long storeId,
        @Schema(description = "업체명", example = "멍카페") String storeName,
        @Schema(description = "광고 유형", example = "BANNER") AdType adType,
        @Schema(description = "노출 시작일", example = "2026-11-15") LocalDate startDate,
        @Schema(description = "노출 종료일", example = "2026-11-30") LocalDate endDate,
        @Schema(description = "광고 소재 URL", example = "https://cdn.pawfit.app/ad/1601.png") String creativeUrl,
        @Schema(description = "신청 메모. 없으면 생략", nullable = true) String message,
        @Schema(description = "처리 상태", example = "IN_REVIEW") ApplicationStatus status,
        @Schema(description = "심사 의견. 심사 전이면 생략", nullable = true) String reviewNote,
        @Schema(description = "마지막 심사 시각. 심사 전이면 생략", nullable = true) Instant reviewedAt,
        @Schema(description = "신청 시각 (UTC)") Instant createdAt) {

    public static AdminAdApplicationDetailResponse of(AdApplication application, String storeName) {
        return new AdminAdApplicationDetailResponse(
                application.getId(),
                application.getStoreId(),
                storeName,
                application.getAdType(),
                application.getStartDate(),
                application.getEndDate(),
                application.getCreativeUrl(),
                application.getMessage(),
                application.getStatus(),
                application.getReviewNote(),
                application.getReviewedAt(),
                application.getCreatedAt());
    }
}
