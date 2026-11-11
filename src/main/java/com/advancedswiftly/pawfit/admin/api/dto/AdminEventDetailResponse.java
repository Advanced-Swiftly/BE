package com.advancedswiftly.pawfit.admin.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "운영자용 이벤트 상세")
public record AdminEventDetailResponse(
        @Schema(description = "이벤트 ID", example = "801") Long eventId,
        @Schema(description = "제목") String title,
        @Schema(description = "연도", example = "2026") int year,
        @Schema(description = "분기", example = "4") int quarter,
        @Schema(description = "상태", example = "OPEN") String status,
        @Schema(description = "시작일", example = "2026-10-01") LocalDate startsOn,
        @Schema(description = "종료일", example = "2026-12-31") LocalDate endsOn,
        @Schema(description = "응모에 필요한 스탬프 수", example = "3") int requiredStampCount,
        @Schema(description = "최대 스탬프 수", example = "5") int maxStampCount,
        @Schema(description = "공지. 없으면 생략", nullable = true) String notice,
        @Schema(description = "대상 지역") List<Region> regions,
        @Schema(description = "경품") List<Prize> prizes,
        @Schema(description = "승인된 참여 업체 수", example = "35") long participatingStores,
        @Schema(description = "응모 수", example = "120") long totalEntries) {

    @Schema(description = "대상 지역")
    public record Region(Long regionId, String name) {
    }

    @Schema(description = "경품")
    public record Prize(Long prizeId, int rank, String name, int quantity) {
    }
}
