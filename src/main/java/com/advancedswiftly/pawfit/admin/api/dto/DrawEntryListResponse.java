package com.advancedswiftly.pawfit.admin.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "추첨 응모자 목록 (최신순)")
public record DrawEntryListResponse(List<DrawEntryListItem> items) {
}
