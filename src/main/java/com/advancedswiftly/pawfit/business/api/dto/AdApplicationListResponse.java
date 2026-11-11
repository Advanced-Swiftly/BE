package com.advancedswiftly.pawfit.business.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "광고 신청 목록")
public record AdApplicationListResponse(List<AdApplicationListItem> items) {
}
