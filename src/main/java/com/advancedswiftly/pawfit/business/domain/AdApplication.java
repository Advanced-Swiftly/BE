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
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "ad_application")
public class AdApplication extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "application_id")
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    /** 신청한 업체 관리자의 user_id. */
    @Column(name = "submitted_by", nullable = false)
    private Long submittedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "ad_type", nullable = false, length = 20)
    private AdType adType;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "creative_url", nullable = false, length = 500)
    private String creativeUrl;

    @Column(name = "message", length = 500)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ApplicationStatus status;

    @Column(name = "review_note", length = 500)
    private String reviewNote;

    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    protected AdApplication() {
    }

    private AdApplication(
            Long storeId,
            Long submittedBy,
            AdType adType,
            LocalDate startDate,
            LocalDate endDate,
            String creativeUrl,
            String message) {
        this.storeId = storeId;
        this.submittedBy = submittedBy;
        this.adType = adType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.creativeUrl = creativeUrl;
        this.message = message;
        this.status = ApplicationStatus.RECEIVED;
    }

    public static AdApplication receive(
            Long storeId,
            Long submittedBy,
            AdType adType,
            LocalDate startDate,
            LocalDate endDate,
            String creativeUrl,
            String message) {
        return new AdApplication(storeId, submittedBy, adType, startDate, endDate, creativeUrl, message);
    }

    public boolean canCancel() {
        return status.isCancelable();
    }

    public void cancel() {
        this.status = ApplicationStatus.CANCELED;
    }

    /** 운영자 심사 결과를 반영한다. comment가 null이면 기존 심사 의견을 유지한다. */
    public void review(ApplicationStatus target, String comment, Long reviewerId) {
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

    public Long getStoreId() {
        return storeId;
    }

    public Long getSubmittedBy() {
        return submittedBy;
    }

    public AdType getAdType() {
        return adType;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public String getCreativeUrl() {
        return creativeUrl;
    }

    public String getMessage() {
        return message;
    }

    public ApplicationStatus getStatus() {
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
