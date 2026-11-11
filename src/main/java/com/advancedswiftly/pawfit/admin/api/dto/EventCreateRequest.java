package com.advancedswiftly.pawfit.admin.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "스탬프 이벤트 생성 요청. 초안(DRAFT)으로 만들어진다")
public record EventCreateRequest(
        @Schema(description = "이벤트명", example = "2026년 4분기 스탬프 투어")
        @NotBlank(message = "이벤트명은 필수입니다.")
        @Size(max = 150, message = "이벤트명은 150자 이하여야 합니다.")
        String title,

        @Schema(description = "이벤트 연도", example = "2026")
        @NotNull(message = "연도는 필수입니다.")
        @Min(value = 2000, message = "연도가 올바르지 않습니다.")
        @Max(value = 2100, message = "연도가 올바르지 않습니다.")
        Integer year,

        @Schema(description = "분기 (1~4)", example = "4")
        @NotNull(message = "분기는 필수입니다.")
        @Min(value = 1, message = "분기는 1~4여야 합니다.")
        @Max(value = 4, message = "분기는 1~4여야 합니다.")
        Integer quarter,

        @Schema(description = "시작일 (yyyy-MM-dd)", example = "2026-10-01")
        @NotNull(message = "시작일은 필수입니다.")
        LocalDate startsOn,

        @Schema(description = "종료일 (yyyy-MM-dd)", example = "2026-12-31")
        @NotNull(message = "종료일은 필수입니다.")
        LocalDate endsOn,

        @Schema(description = "응모에 필요한 스탬프 수", example = "3")
        @NotNull(message = "필요 스탬프 수는 필수입니다.")
        @Min(value = 1, message = "필요 스탬프 수는 1 이상이어야 합니다.")
        Integer requiredStampCount,

        @Schema(description = "받을 수 있는 최대 스탬프 수", example = "5")
        @NotNull(message = "최대 스탬프 수는 필수입니다.")
        @Min(value = 1, message = "최대 스탬프 수는 1 이상이어야 합니다.")
        Integer maxStampCount,

        @Schema(description = "안내 문구", nullable = true)
        @Size(max = 2000, message = "안내 문구는 2000자 이하여야 합니다.")
        String notice,

        @Schema(description = "적용 지역 ID")
        @NotEmpty(message = "적용 지역은 비어 있을 수 없습니다.")
        @Size(max = 50, message = "적용 지역은 50개 이하여야 합니다.")
        List<@NotNull(message = "지역 ID는 비어 있을 수 없습니다.") Long> regionIds,

        @Schema(description = "경품 목록")
        @NotEmpty(message = "경품은 비어 있을 수 없습니다.")
        @Size(max = 20, message = "경품은 20개 이하여야 합니다.")
        List<@NotNull(message = "경품 항목은 비어 있을 수 없습니다.") @Valid EventPrizeRequest> prizes) {
}
