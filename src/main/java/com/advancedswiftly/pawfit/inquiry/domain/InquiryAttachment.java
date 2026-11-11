package com.advancedswiftly.pawfit.inquiry.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "inquiry_attachment")
public class InquiryAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inquiry_attachment_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inquiry_id", nullable = false)
    private Inquiry inquiry;

    @Column(name = "attachment_url", nullable = false, length = 500)
    private String attachmentUrl;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    protected InquiryAttachment() {
    }

    private InquiryAttachment(Inquiry inquiry, String attachmentUrl, int displayOrder) {
        this.inquiry = inquiry;
        this.attachmentUrl = attachmentUrl;
        this.displayOrder = displayOrder;
    }

    static InquiryAttachment create(
            Inquiry inquiry, String attachmentUrl, int displayOrder) {
        return new InquiryAttachment(inquiry, attachmentUrl, displayOrder);
    }

    public String getAttachmentUrl() {
        return attachmentUrl;
    }
}
