package com.advancedswiftly.pawfit.admin.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

@Schema(description = "운영자 업체 직접 등록 요청")
public record AdminStoreCreateRequest(
        @Schema(description = "업체명", example = "멍카페")
        @NotBlank(message = "업체명은 필수입니다.")
        @Size(max = 100, message = "업체명은 100자 이하여야 합니다.")
        String name,

        @Schema(description = "주소", example = "서울특별시 성동구 성수이로 1")
        @NotBlank(message = "주소는 필수입니다.")
        @Size(max = 255, message = "주소는 255자 이하여야 합니다.")
        String address,

        @Schema(description = "위도", example = "37.5446")
        @NotNull(message = "위도는 필수입니다.")
        @DecimalMin(value = "-90", message = "위도가 올바르지 않습니다.")
        @DecimalMax(value = "90", message = "위도가 올바르지 않습니다.")
        BigDecimal latitude,

        @Schema(description = "경도", example = "127.0557")
        @NotNull(message = "경도는 필수입니다.")
        @DecimalMin(value = "-180", message = "경도가 올바르지 않습니다.")
        @DecimalMax(value = "180", message = "경도가 올바르지 않습니다.")
        BigDecimal longitude,

        @Schema(description = "대표 연락처", example = "02-1234-5678", nullable = true)
        @Size(max = 20, message = "연락처는 20자 이하여야 합니다.")
        String phoneNumber,

        @Schema(description = "지역 ID", example = "11")
        @NotNull(message = "지역은 필수입니다.")
        Long regionId) {
}
