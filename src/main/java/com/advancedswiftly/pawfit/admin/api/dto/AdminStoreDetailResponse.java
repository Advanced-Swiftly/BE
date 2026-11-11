package com.advancedswiftly.pawfit.admin.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "운영자 업체 상세")
public record AdminStoreDetailResponse(
        @Schema(description = "업체 ID", example = "100") Long storeId,
        @Schema(description = "업체명") String name,
        @Schema(description = "주소") String address,
        @Schema(description = "대표 연락처. 없으면 생략", nullable = true) String phoneNumber,
        @Schema(description = "업체 소개. 없으면 생략", nullable = true) String description,
        @Schema(description = "지역 ID. 지정 전이면 생략", nullable = true) Long regionId,
        @Schema(description = "노출 상태", example = "VISIBLE") String visibility,
        @Schema(description = "검증 상태", example = "VERIFIED") String verificationStatus,
        @Schema(description = "업체 관리자. 없으면 생략", nullable = true) Manager manager,
        @Schema(description = "적용 중인 정책 버전. 정책이 없으면 생략", example = "3", nullable = true) Integer policyVersion,
        @Schema(description = "처리 대기 중인 조건 요청 수 (접수·심사 중)", example = "1") long pendingRequests,
        @Schema(description = "현재 QR 상태. QR이 없으면 생략", example = "ACTIVE", nullable = true) String qrStatus) {

    @Schema(description = "업체 관리자")
    public record Manager(
            @Schema(description = "사용자 ID", example = "2") Long userId,
            @Schema(description = "이름. 사용자를 찾을 수 없으면 생략", nullable = true) String name) {
    }
}
