package com.advancedswiftly.pawfit.business.api.dto;

import com.advancedswiftly.pawfit.business.domain.QrStatus;
import com.advancedswiftly.pawfit.business.domain.StoreQr;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "업체 QR")
public record StoreQrResponse(
        @Schema(description = "QR ID", example = "1401") Long qrId,
        @Schema(description = "QR 상태", example = "ACTIVE") QrStatus status,
        @Schema(description = "표시용 QR 이미지 URL. 상태 변경 응답에는 없습니다", example = "https://cdn.pawfit.app/qr/1401.png", nullable = true)
        String qrImageUrl) {

    public static StoreQrResponse withImage(StoreQr qr) {
        return new StoreQrResponse(qr.getId(), qr.getStatus(), qr.getQrImageUrl());
    }

    public static StoreQrResponse statusOnly(StoreQr qr) {
        return new StoreQrResponse(qr.getId(), qr.getStatus(), null);
    }
}
