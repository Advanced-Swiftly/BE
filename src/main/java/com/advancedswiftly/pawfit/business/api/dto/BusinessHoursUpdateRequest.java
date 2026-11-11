package com.advancedswiftly.pawfit.business.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "영업시간 일괄 수정 요청. 전달한 요일만 변경한다")
public record BusinessHoursUpdateRequest(
        @Schema(description = "요일·시작·종료·휴무 여부 목록 (같은 요일은 한 번만)")
        @NotEmpty(message = "영업시간 목록은 비어 있을 수 없습니다.")
        @Size(max = 7, message = "영업시간은 요일별로 최대 7개입니다.")
        List<@NotNull(message = "영업시간 항목은 비어 있을 수 없습니다.") @Valid BusinessHoursUpdateItem> hours) {
}
