package com.advancedswiftly.pawfit.admin.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 문의 답변에 붙는 첨부파일. */
@Entity
@Table(name = "inquiry_answer_attachment")
public class InquiryAnswerAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attachment_id")
    private Long id;

    @Column(name = "inquiry_id", nullable = false)
    private Long inquiryId;

    @Column(name = "attachment_url", nullable = false, length = 500)
    private String attachmentUrl;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    protected InquiryAnswerAttachment() {
    }

    public InquiryAnswerAttachment(Long inquiryId, String attachmentUrl, int displayOrder) {
        this.inquiryId = inquiryId;
        this.attachmentUrl = attachmentUrl;
        this.displayOrder = displayOrder;
    }
}
