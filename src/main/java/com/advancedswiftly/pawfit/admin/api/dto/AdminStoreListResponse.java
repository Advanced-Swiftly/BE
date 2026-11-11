package com.advancedswiftly.pawfit.admin.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "운영자 업체 목록 (최신순)")
public record AdminStoreListResponse(List<AdminStoreListItem> items) {
}
