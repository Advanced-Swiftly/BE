package com.advancedswiftly.pawfit.business.domain;

import com.advancedswiftly.pawfit.global.common.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "event_application")
public class EventApplication extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "application_id")
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    /** 신청한 업체 관리자의 user_id. */
    @Column(name = "submitted_by", nullable = false)
    private Long submittedBy;

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

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "event_application_region", joinColumns = @JoinColumn(name = "application_id"))
    @Column(name = "region_id", nullable = false)
    private Set<Long> regionIds = new LinkedHashSet<>();

    protected EventApplication() {
    }

    private EventApplication(
            Long storeId, Long eventId, Long submittedBy, Set<Long> regionIds, String message) {
        this.storeId = storeId;
        this.eventId = eventId;
        this.submittedBy = submittedBy;
        this.regionIds = new LinkedHashSet<>(regionIds);
        this.message = message;
        this.status = ApplicationStatus.RECEIVED;
    }

    public static EventApplication receive(
            Long storeId, Long eventId, Long submittedBy, Set<Long> regionIds, String message) {
        return new EventApplication(storeId, eventId, submittedBy, regionIds, message);
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

    public Long getEventId() {
        return eventId;
    }

    public Long getSubmittedBy() {
        return submittedBy;
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

    public Set<Long> getRegionIds() {
        return Collections.unmodifiableSet(regionIds);
    }
}
