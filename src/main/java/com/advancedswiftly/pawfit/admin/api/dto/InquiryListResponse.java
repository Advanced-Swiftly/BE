package com.advancedswiftly.pawfit.admin.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "문의 목록 (최신순)")
public record InquiryListResponse(List<InquiryListItem> items) {
}
