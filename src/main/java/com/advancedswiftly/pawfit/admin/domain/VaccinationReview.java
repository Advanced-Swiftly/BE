package com.advancedswiftly.pawfit.admin.domain;

import com.advancedswiftly.pawfit.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 예방접종 검토 처리에서 운영자가 남긴 사유. 기록 한 건에 하나씩 둔다. */
@Entity
@Table(name = "vaccination_review")
public class VaccinationReview extends BaseEntity {

    @Id
    @Column(name = "record_id")
    private Long recordId;

    @Column(name = "review_comment", length = 500)
    private String reviewComment;

    @Column(name = "reviewed_by")
    private Long reviewedBy;

    protected VaccinationReview() {
    }

    public VaccinationReview(Long recordId) {
        this.recordId = recordId;
    }

    public void record(String reviewComment, Long reviewedBy) {
        this.reviewComment = reviewComment;
        this.reviewedBy = reviewedBy;
    }

    public Long getRecordId() {
        return recordId;
    }

    public String getReviewComment() {
        return reviewComment;
    }
}
