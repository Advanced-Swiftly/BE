package com.advancedswiftly.pawfit.business.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/** 업체 기본 정보 수정. 값이 전달된 항목만 수정하고, 생략하면 기존 값을 유지한다. */
@Schema(description = "업체 기본 정보 수정 요청 (전달한 항목만 수정)")
public record StoreProfileUpdateRequest(
        @Schema(description = "업체명", example = "멍카페", nullable = true)
        @Pattern(regexp = ".*\\S.*", message = "업체명은 공백일 수 없습니다.")
        @Size(max = 100, message = "업체명은 100자 이하여야 합니다.")
        String name,

        @Schema(description = "대표 연락처", example = "02-1234-5678", nullable = true)
        @Pattern(regexp = ".*\\S.*", message = "연락처는 공백일 수 없습니다.")
        @Size(max = 20, message = "연락처는 20자 이하여야 합니다.")
        String phoneNumber,

        @Schema(description = "업체 소개", example = "반려견과 함께하는 브런치 카페", nullable = true)
        @Size(max = 1000, message = "업체 소개는 1000자 이하여야 합니다.")
        String description,

        @Schema(description = "편의시설 코드 목록. 전달하면 전체를 교체하고, 빈 배열이면 모두 제거", example = "[\"PARKING\", \"TERRACE\"]", nullable = true)
        @Size(max = 30, message = "편의시설은 30개 이하여야 합니다.")
        List<@NotBlank(message = "편의시설 코드는 비어 있을 수 없습니다.")
                @Size(max = 50, message = "편의시설 코드는 50자 이하여야 합니다.") String> amenities) {
}
