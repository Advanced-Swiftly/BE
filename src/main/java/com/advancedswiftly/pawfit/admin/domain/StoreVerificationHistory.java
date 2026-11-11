package com.advancedswiftly.pawfit.admin.domain;

import com.advancedswiftly.pawfit.business.domain.VerificationStatus;
import com.advancedswiftly.pawfit.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 사업자 인증 심사에서 상태가 바뀔 때마다 남기는 이력. */
@Entity
@Table(name = "store_verification_history")
public class StoreVerificationHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id")
    private Long id;

    @Column(name = "verification_id", nullable = false)
    private Long verificationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private VerificationStatus status;

    @Column(name = "comment", length = 500)
    private String comment;

    @Column(name = "changed_by")
    private Long changedBy;

    protected StoreVerificationHistory() {
    }

    public StoreVerificationHistory(Long verificationId, VerificationStatus status, String comment, Long changedBy) {
        this.verificationId = verificationId;
        this.status = status;
        this.comment = comment;
        this.changedBy = changedBy;
    }

    public Long getId() {
        return id;
    }

    public VerificationStatus getStatus() {
        return status;
    }

    public String getComment() {
        return comment;
    }
}
