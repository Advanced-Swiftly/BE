package com.advancedswiftly.pawfit.admin.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "운영자 업체 정보 수정 요청. 전달한 항목만 바뀝니다")
public record AdminStoreUpdateRequest(
        @Schema(description = "업체명", nullable = true)
        @Pattern(regexp = ".*\\S.*", message = "업체명은 공백일 수 없습니다.")
        @Size(max = 100, message = "업체명은 100자 이하여야 합니다.")
        String name,

        @Schema(description = "주소", nullable = true)
        @Pattern(regexp = ".*\\S.*", message = "주소는 공백일 수 없습니다.")
        @Size(max = 255, message = "주소는 255자 이하여야 합니다.")
        String address,

        @Schema(description = "업체 관리자로 지정할 사용자 ID. 업체 관리자 역할이어야 함", nullable = true)
        Long managerUserId,

        @Schema(description = "업체 소개", nullable = true)
        @Size(max = 1000, message = "업체 소개는 1000자 이하여야 합니다.")
        String description) {
}
