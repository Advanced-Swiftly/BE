package com.advancedswiftly.pawfit.business.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

@Schema(description = "인증을 요청하는 업체 정보")
public record StoreRequest(
        @Schema(description = "업체명", example = "성수 댕댕카페")
        @NotBlank(message = "업체명은 필수입니다.")
        @Size(max = 100, message = "업체명은 100자 이하여야 합니다.")
        String name,

        @Schema(description = "업체 주소", example = "서울특별시 성동구 성수이로 1")
        @NotBlank(message = "주소는 필수입니다.")
        @Size(max = 255, message = "주소는 255자 이하여야 합니다.")
        String address,

        @Schema(description = "업체 연락처", example = "02-1234-5678")
        @NotBlank(message = "연락처는 필수입니다.")
        @Size(max = 20, message = "연락처는 20자 이하여야 합니다.")
        String phone,

        @Schema(description = "위도", example = "37.5446")
        @NotNull(message = "위도는 필수입니다.")
        @DecimalMin(value = "-90.0", message = "위도는 -90 이상이어야 합니다.")
        @DecimalMax(value = "90.0", message = "위도는 90 이하여야 합니다.")
        @Digits(integer = 3, fraction = 7, message = "위도 형식이 올바르지 않습니다.")
        BigDecimal latitude,

        @Schema(description = "경도", example = "127.0557")
        @NotNull(message = "경도는 필수입니다.")
        @DecimalMin(value = "-180.0", message = "경도는 -180 이상이어야 합니다.")
        @DecimalMax(value = "180.0", message = "경도는 180 이하여야 합니다.")
        @Digits(integer = 3, fraction = 7, message = "경도 형식이 올바르지 않습니다.")
        BigDecimal longitude) {
}
