package com.advancedswiftly.pawfit.business.domain;

import com.advancedswiftly.pawfit.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "store_qr")
public class StoreQr extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "qr_id")
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private QrStatus status;

    @Column(name = "qr_image_url", nullable = false, length = 500)
    private String qrImageUrl;

    protected StoreQr() {
    }

    private StoreQr(Long storeId, String qrImageUrl) {
        this.storeId = storeId;
        this.qrImageUrl = qrImageUrl;
        this.status = QrStatus.ACTIVE;
    }

    public static StoreQr issue(Long storeId, String qrImageUrl) {
        return new StoreQr(storeId, qrImageUrl);
    }

    public void changeActive(boolean active) {
        this.status = active ? QrStatus.ACTIVE : QrStatus.DISABLED;
    }

    public Long getId() {
        return id;
    }

    public Long getStoreId() {
        return storeId;
    }

    public QrStatus getStatus() {
        return status;
    }

    public String getQrImageUrl() {
        return qrImageUrl;
    }
}
