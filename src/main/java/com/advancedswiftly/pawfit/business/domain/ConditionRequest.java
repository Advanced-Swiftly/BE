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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 업체가 제출한 입장 조건 변경 요청. */
@Entity
@Table(name = "condition_request")
public class ConditionRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_id")
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    /** 변경의 기준이 된 정책 버전. 정책이 없으면 0. */
    @Column(name = "base_version", nullable = false)
    private int baseVersion;

    @Column(name = "source_text", nullable = false, length = 2000)
    private String sourceText;

    /** 요청한 업체 관리자의 user_id. */
    @Column(name = "submitted_by", nullable = false)
    private Long submittedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ConditionRequestStatus status;

    @Column(name = "review_note", length = 500)
    private String reviewNote;

    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "condition_request_rule", joinColumns = @JoinColumn(name = "request_id"))
    private List<PolicyRule> requestedRules = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "condition_request_evidence", joinColumns = @JoinColumn(name = "request_id"))
    @Column(name = "evidence_url", nullable = false, length = 500)
    private List<String> evidenceUrls = new ArrayList<>();

    protected ConditionRequest() {
    }

    private ConditionRequest(
            Long storeId,
            int baseVersion,
            String sourceText,
            Long submittedBy,
            List<PolicyRule> requestedRules,
            List<String> evidenceUrls) {
        this.storeId = storeId;
        this.baseVersion = baseVersion;
        this.sourceText = sourceText;
        this.submittedBy = submittedBy;
        this.requestedRules = new ArrayList<>(requestedRules);
        this.evidenceUrls = new ArrayList<>(evidenceUrls);
        this.status = ConditionRequestStatus.RECEIVED;
    }

    public static ConditionRequest receive(
            Long storeId,
            int baseVersion,
            String sourceText,
            Long submittedBy,
            List<PolicyRule> requestedRules,
            List<String> evidenceUrls) {
        return new ConditionRequest(storeId, baseVersion, sourceText, submittedBy, requestedRules, evidenceUrls);
    }

    /** 대기 중(접수됨 또는 보완 요청됨)인 요청만 취소할 수 있다. */
    public boolean canCancel() {
        return status == ConditionRequestStatus.RECEIVED || status == ConditionRequestStatus.NEEDS_SUPPLEMENT;
    }

    /** 보완 요청(NEEDS_SUPPLEMENT)을 받은 요청만 보완 제출할 수 있다. */
    public boolean canResubmit() {
        return status == ConditionRequestStatus.NEEDS_SUPPLEMENT;
    }

    public void cancel() {
        this.status = ConditionRequestStatus.CANCELED;
    }

    /**
     * 보완 내용을 반영하고 다시 접수(RECEIVED) 상태로 돌린다.
     * null인 항목은 유지하고, 근거 URL은 기존 목록에 없는 것만 추가한다.
     */
    public void resubmit(List<PolicyRule> rules, String sourceText, List<String> additionalEvidenceUrls) {
        if (rules != null) {
            this.requestedRules.clear();
            this.requestedRules.addAll(rules);
        }
        if (sourceText != null) {
            this.sourceText = sourceText;
        }
        if (additionalEvidenceUrls != null) {
            for (String url : additionalEvidenceUrls) {
                if (!this.evidenceUrls.contains(url)) {
                    this.evidenceUrls.add(url);
                }
            }
        }
        this.status = ConditionRequestStatus.RECEIVED;
    }

    public Long getId() {
        return id;
    }

    public Long getStoreId() {
        return storeId;
    }

    public int getBaseVersion() {
        return baseVersion;
    }

    public String getSourceText() {
        return sourceText;
    }

    public Long getSubmittedBy() {
        return submittedBy;
    }

    public ConditionRequestStatus getStatus() {
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

    public List<PolicyRule> getRequestedRules() {
        return Collections.unmodifiableList(requestedRules);
    }

    public List<String> getEvidenceUrls() {
        return Collections.unmodifiableList(evidenceUrls);
    }
}
