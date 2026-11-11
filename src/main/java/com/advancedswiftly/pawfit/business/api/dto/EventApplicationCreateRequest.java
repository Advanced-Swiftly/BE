package com.advancedswiftly.pawfit.business.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "이벤트 참여 신청 요청")
public record EventApplicationCreateRequest(
        @Schema(description = "신청 업체 ID", example = "11")
        @NotNull(message = "업체 ID는 필수입니다.")
        Long storeId,

        @Schema(description = "이벤트 ID", example = "801")
        @NotNull(message = "이벤트 ID는 필수입니다.")
        Long eventId,

        @Schema(description = "참여 희망 지역 ID 목록 (이벤트 참여 지역 안에서 선택)", example = "[11, 41]")
        @NotEmpty(message = "참여 희망 지역은 1개 이상이어야 합니다.")
        @Size(max = 50, message = "참여 희망 지역은 50개 이하여야 합니다.")
        List<@NotNull(message = "지역 ID는 비어 있을 수 없습니다.") Long> regionIds,

        @Schema(description = "운영자 전달사항", example = "주말 운영 업체입니다.", nullable = true)
        @Size(max = 500, message = "전달사항은 500자 이하여야 합니다.")
        String message) {
}
