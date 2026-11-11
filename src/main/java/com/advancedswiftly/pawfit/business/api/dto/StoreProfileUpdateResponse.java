package com.advancedswiftly.pawfit.business.api.dto;

import com.advancedswiftly.pawfit.business.domain.Store;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "업체 기본 정보 수정 결과")
public record StoreProfileUpdateResponse(
        @Schema(description = "업체 ID", example = "100") Long storeId,
        @Schema(description = "업체명", example = "멍카페") String name) {

    public static StoreProfileUpdateResponse from(Store store) {
        return new StoreProfileUpdateResponse(store.getId(), store.getName());
    }
}
