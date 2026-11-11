package com.advancedswiftly.pawfit.admin.api.dto;

import com.advancedswiftly.pawfit.business.domain.VerificationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

@Schema(description = "사업자 인증 심사 상세")
public record AdminVerificationDetailResponse(
        @Schema(description = "인증 요청 ID", example = "1101") Long verificationId,
        @Schema(description = "처리 상태", example = "IN_REVIEW") VerificationStatus status,
        @Schema(description = "업체 ID", example = "100") Long storeId,
        @Schema(description = "업체명") String storeName,
        @Schema(description = "업체 주소") String storeAddress,
        @Schema(description = "업체 연락처. 없으면 생략", nullable = true) String storePhone,
        @Schema(description = "사업자등록번호 (숫자 10자리)", example = "1234567890") String businessNumber,
        @Schema(description = "상호명") String businessName,
        @Schema(description = "대표자명") String representativeName,
        @Schema(description = "제출한 사업자등록증 경로") String documentUrl,
        @Schema(description = "요청한 업체 관리자 ID", example = "2") Long submittedBy,
        @Schema(description = "심사 의견. 심사 전이면 생략", nullable = true) String reviewComment,
        @Schema(description = "상태 이력 (오래된 순). 첫 항목은 접수") List<StatusHistory> statusHistory) {

    @Schema(description = "상태 이력 하나")
    public record StatusHistory(
            @Schema(description = "상태", example = "RECEIVED") VerificationStatus status,
            @Schema(description = "사유. 없으면 생략", nullable = true) String comment,
            @Schema(description = "변경 시각 (UTC)") Instant changedAt) {
    }
}
