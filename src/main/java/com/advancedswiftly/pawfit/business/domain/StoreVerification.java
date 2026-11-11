package com.advancedswiftly.pawfit.business.domain;

import com.advancedswiftly.pawfit.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "store_verification")
public class StoreVerification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "verification_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    /** 요청한 업체 관리자의 user_id. users 테이블 FK는 users migration 이후 추가한다. */
    @Column(name = "submitted_by", nullable = false)
    private Long submittedBy;

    /** 하이픈을 제거한 숫자 10자리. */
    @Column(name = "business_number", nullable = false, length = 10)
    private String businessNumber;

    @Column(name = "business_name", nullable = false, length = 100)
    private String businessName;

    @Column(name = "representative_name", nullable = false, length = 50)
    private String representativeName;

    @Column(name = "document_url", nullable = false, length = 500)
    private String documentUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private VerificationStatus status;

    @Column(name = "review_note", length = 500)
    private String reviewNote;

    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    protected StoreVerification() {
    }

    private StoreVerification(
            Store store,
            Long submittedBy,
            String businessNumber,
            String businessName,
            String representativeName,
            String documentUrl) {
        this.store = store;
        this.submittedBy = submittedBy;
        this.businessNumber = businessNumber;
        this.businessName = businessName;
        this.representativeName = representativeName;
        this.documentUrl = documentUrl;
        this.status = VerificationStatus.RECEIVED;
    }

    public static StoreVerification receive(
            Store store,
            Long submittedBy,
            String businessNumber,
            String businessName,
            String representativeName,
            String documentUrl) {
        return new StoreVerification(
                store, submittedBy, businessNumber, businessName, representativeName, documentUrl);
    }

    /** 보완 요청(NEEDS_SUPPLEMENT)을 받은 건만 재제출할 수 있다. */
    public boolean canResubmit() {
        return status == VerificationStatus.NEEDS_SUPPLEMENT;
    }

    /** 새 사업자등록증으로 교체하고 다시 심사 대기(RECEIVED) 상태로 돌린다. */
    public void resubmit(String documentUrl) {
        this.documentUrl = documentUrl;
        this.status = VerificationStatus.RECEIVED;
    }

    /** 운영자 심사 결과를 반영한다. comment가 null이면 기존 심사 의견을 유지한다. */
    public void review(VerificationStatus target, String comment, Long reviewerId) {
        this.status = target;
        if (comment != null) {
            this.reviewNote = comment;
        }
        this.reviewedBy = reviewerId;
        this.reviewedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Store getStore() {
        return store;
    }

    public Long getSubmittedBy() {
        return submittedBy;
    }

    public String getBusinessNumber() {
        return businessNumber;
    }

    public String getBusinessName() {
        return businessName;
    }

    public String getRepresentativeName() {
        return representativeName;
    }

    public String getDocumentUrl() {
        return documentUrl;
    }

    public VerificationStatus getStatus() {
        return status;
    }

    public String getReviewNote() {
        return reviewNote;
    }

    public Long getReviewedBy() {
        return reviewedBy;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }
}
