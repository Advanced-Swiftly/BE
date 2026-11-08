package com.advancedswiftly.pawfit.inquiry.domain;

import com.advancedswiftly.pawfit.auth.domain.User;
import com.advancedswiftly.pawfit.global.common.BaseEntity;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "inquiry")
public class Inquiry extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inquiry_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    private InquiryCategory category;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "content", nullable = false, length = 4000)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private InquiryStatus status;

    @Column(name = "answer", length = 4000)
    private String answer;

    @Column(name = "answered_at")
    private Instant answeredAt;

    @OrderBy("displayOrder ASC")
    @OneToMany(mappedBy = "inquiry", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InquiryAttachment> attachments = new ArrayList<>();

    protected Inquiry() {
    }

    private Inquiry(User user, InquiryCategory category, String title, String content) {
        this.user = user;
        this.category = category;
        this.title = title;
        this.content = content;
        this.status = InquiryStatus.WAITING;
    }

    public static Inquiry create(
            User user,
            InquiryCategory category,
            String title,
            String content,
            List<String> attachmentUrls) {
        Inquiry inquiry = new Inquiry(user, category, title.trim(), content.trim());
        for (int index = 0; index < attachmentUrls.size(); index++) {
            inquiry.attachments.add(InquiryAttachment.create(
                    inquiry, attachmentUrls.get(index), index));
        }
        return inquiry;
    }

    public Long getId() {
        return id;
    }

    public InquiryCategory getCategory() {
        return category;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public InquiryStatus getStatus() {
        return status;
    }

    public String getAnswer() {
        return answer;
    }

    public Instant getAnsweredAt() {
        return answeredAt;
    }

    public List<InquiryAttachment> getAttachments() {
        return Collections.unmodifiableList(attachments);
    }
}
