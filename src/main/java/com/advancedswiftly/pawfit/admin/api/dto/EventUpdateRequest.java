package com.advancedswiftly.pawfit.admin.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "초안 이벤트 수정 요청. 전달한 항목만 바꾸고, regionIds·prizes는 목록 전체를 교체한다")
public record EventUpdateRequest(
        @Schema(description = "이벤트명", nullable = true)
        @Pattern(regexp = ".*\\S.*", message = "이벤트명은 공백일 수 없습니다.")
        @Size(max = 150, message = "이벤트명은 150자 이하여야 합니다.")
        String title,

        @Schema(description = "시작일 (yyyy-MM-dd)", nullable = true) LocalDate startsOn,
        @Schema(description = "종료일 (yyyy-MM-dd)", nullable = true) LocalDate endsOn,

        @Schema(description = "응모에 필요한 스탬프 수", nullable = true)
        @Min(value = 1, message = "필요 스탬프 수는 1 이상이어야 합니다.")
        Integer requiredStampCount,

        @Schema(description = "받을 수 있는 최대 스탬프 수", nullable = true)
        @Min(value = 1, message = "최대 스탬프 수는 1 이상이어야 합니다.")
        Integer maxStampCount,

        @Schema(description = "안내 문구", nullable = true)
        @Size(max = 2000, message = "안내 문구는 2000자 이하여야 합니다.")
        String notice,

        @Schema(description = "적용 지역 ID (전체 교체)", nullable = true)
        @Size(min = 1, max = 50, message = "적용 지역은 1~50개여야 합니다.")
        List<@NotNull(message = "지역 ID는 비어 있을 수 없습니다.") Long> regionIds,

        @Schema(description = "경품 목록 (전체 교체)", nullable = true)
        @Size(min = 1, max = 20, message = "경품은 1~20개여야 합니다.")
        List<@NotNull(message = "경품 항목은 비어 있을 수 없습니다.") @Valid EventPrizeRequest> prizes,

        @Schema(description = "OPEN으로 보내면 초안을 공개한다. 지역과 경품이 있어야 한다", nullable = true, allowableValues = {"OPEN"})
        String status) {
}
