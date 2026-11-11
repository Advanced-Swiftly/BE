package com.advancedswiftly.pawfit.business.api.dto;

import com.advancedswiftly.pawfit.business.domain.Store;
import com.advancedswiftly.pawfit.business.domain.StoreVerificationStatus;
import com.advancedswiftly.pawfit.business.domain.StoreVisibility;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "관리 업체 목록의 항목")
public record ManagedStoreListItem(
        @Schema(description = "업체 ID", example = "100") Long storeId,
        @Schema(description = "업체명", example = "멍카페") String name,
        @Schema(description = "노출 상태. 인증이 완료되고 운영자가 숨기지 않은 업체는 VISIBLE, 아니면 HIDDEN", example = "VISIBLE") String visibility) {

    public static ManagedStoreListItem from(Store store) {
        boolean visible = store.getVerificationStatus() == StoreVerificationStatus.VERIFIED
                && store.getVisibility() == StoreVisibility.VISIBLE;
        String visibility = visible ? "VISIBLE" : "HIDDEN";
        return new ManagedStoreListItem(store.getId(), store.getName(), visibility);
    }
}
