package com.advancedswiftly.pawfit.business.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "업체 영업시간")
public record BusinessHoursResponse(
        @Schema(description = "요일별 영업시간 (월요일부터). 등록된 영업시간이 없으면 빈 배열") List<BusinessHoursItem> hours) {
}
