package com.advancedswiftly.pawfit.admin.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "예방접종 검토 대상 목록 (최신순)")
public record VaccinationReviewListResponse(List<VaccinationReviewListItem> items) {
}
