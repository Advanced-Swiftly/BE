package com.advancedswiftly.pawfit.admin.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "이벤트 추첨 결과")
public record DrawResultResponse(
        @Schema(description = "이벤트 ID", example = "801") Long eventId,
        @Schema(description = "경품 순위순 당첨자 (당첨 취소 포함)") List<Winner> winners) {

    @Schema(description = "당첨자")
    public record Winner(
            @Schema(description = "응모 ID", example = "1001") Long drawEntryId,
            @Schema(description = "사용자 ID", example = "1") Long userId,
            @Schema(description = "경품 순위. 경품이 정해지지 않았으면 생략", nullable = true) Integer prizeRank,
            @Schema(description = "경품명. 경품이 정해지지 않았으면 생략", example = "펫 유모차", nullable = true) String prizeName,
            @Schema(description = "응모 상태. 당첨은 WON, 당첨 취소는 CANCELED", example = "WON") String status,
            @Schema(description = "연락·배송 상태. 없으면 생략", example = "CONTACT_PENDING", nullable = true) String fulfillmentStatus,
            @Schema(description = "운송장 번호. 없으면 생략", nullable = true) String trackingNumber) {
    }
}
