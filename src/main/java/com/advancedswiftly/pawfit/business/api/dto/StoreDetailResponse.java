package com.advancedswiftly.pawfit.business.api.dto;

import com.advancedswiftly.pawfit.business.domain.QrStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "관리 업체 상세")
public record StoreDetailResponse(
        @Schema(description = "업체 ID", example = "100") Long storeId,
        @Schema(description = "업체명", example = "멍카페") String name,
        @Schema(description = "대표 연락처", example = "02-1234-5678", nullable = true) String phoneNumber,
        @Schema(description = "주소", example = "서울특별시 성동구 성수이로 1") String address,
        @Schema(description = "업체 소개", nullable = true) String description,
        @Schema(description = "편의시설 코드") List<String> amenities,
        @Schema(description = "요일별 영업시간 (월요일부터)") List<BusinessHoursItem> hours,
        @Schema(description = "적용 중인 정책 버전. 정책이 없으면 생략", example = "3", nullable = true) Integer policyVersion,
        @Schema(description = "현재 QR 상태. QR이 없으면 생략", example = "ACTIVE", nullable = true) QrStatus qrStatus) {
}
