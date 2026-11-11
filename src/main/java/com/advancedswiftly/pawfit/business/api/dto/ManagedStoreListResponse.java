package com.advancedswiftly.pawfit.business.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "내 관리 업체 목록")
public record ManagedStoreListResponse(List<ManagedStoreListItem> stores) {
}
