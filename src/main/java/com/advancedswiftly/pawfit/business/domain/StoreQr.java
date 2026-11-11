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

    /** QR 이미지에 담기는 원본 토큰. 방문 인증에는 이 토큰의 SHA-256 해시를 쓴다. */
    @Column(name = "qr_token", length = 64)
    private String qrToken;

    @Column(name = "qr_token_hash", length = 64, unique = true)
    private String qrTokenHash;

    @Column(name = "issue_reason", length = 200)
    private String issueReason;

    protected StoreQr() {
    }

    private StoreQr(
            Long storeId, String qrImageUrl, String qrToken, String qrTokenHash, String issueReason) {
        this.storeId = storeId;
        this.qrImageUrl = qrImageUrl;
        this.qrToken = qrToken;
        this.qrTokenHash = qrTokenHash;
        this.issueReason = issueReason;
        this.status = QrStatus.ACTIVE;
    }

    /** 토큰 없이 이미지 URL만 가진 QR. 방문 인증과 연결되지 않으므로 테스트 데이터 등에만 쓴다. */
    public static StoreQr issue(Long storeId, String qrImageUrl) {
        return new StoreQr(storeId, qrImageUrl, null, null, null);
    }

    public static StoreQr issue(
            Long storeId, String qrImageUrl, String qrToken, String qrTokenHash, String issueReason) {
        return new StoreQr(storeId, qrImageUrl, qrToken, qrTokenHash, issueReason);
    }

    /** 재발급으로 기존 QR을 폐기한다. 폐기된 QR은 다시 활성화할 수 없다. */
    public void revoke() {
        this.status = QrStatus.REVOKED;
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

    public String getQrTokenHash() {
        return qrTokenHash;
    }

    public String getIssueReason() {
        return issueReason;
    }

    public QrStatus getStatus() {
        return status;
    }

    public String getQrImageUrl() {
        return qrImageUrl;
    }
}
