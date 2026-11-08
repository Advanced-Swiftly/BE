package com.advancedswiftly.pawfit.auth.domain;

import com.advancedswiftly.pawfit.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(name = "app_user")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(name = "phone_number", nullable = false, unique = true, length = 20)
    private String phoneNumber;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatus status;

    @Column(name = "notify_event", nullable = false)
    @ColumnDefault("true")
    private boolean notifyEvent = true;
    @Column(name = "notify_inquiry", nullable = false)
    @ColumnDefault("true")
    private boolean notifyInquiry = true;
    @Column(name = "notify_review_result", nullable = false)
    @ColumnDefault("true")
    private boolean notifyReviewResult = true;
    @Column(name = "notify_marketing", nullable = false)
    @ColumnDefault("false")
    private boolean notifyMarketing;
    @Column(name = "withdrawal_reason", length = 500)
    private String withdrawalReason;

    public void updateProfile(String name, String phoneNumber) {
        if (name != null) this.name = name.strip();
        if (phoneNumber != null) this.phoneNumber = phoneNumber;
    }

    public void updateNotifications(Boolean event, Boolean inquiry, Boolean reviewResult, Boolean marketing) {
        if (event != null) notifyEvent = event;
        if (inquiry != null) notifyInquiry = inquiry;
        if (reviewResult != null) notifyReviewResult = reviewResult;
        if (marketing != null) notifyMarketing = marketing;
    }

    public boolean isNotifyEvent() { return notifyEvent; }
    public boolean isNotifyInquiry() { return notifyInquiry; }
    public boolean isNotifyReviewResult() { return notifyReviewResult; }
    public boolean isNotifyMarketing() { return notifyMarketing; }

    public void withdraw(String reason) {
        status = UserStatus.WITHDRAWN;
        withdrawalReason = reason;
        updateNotifications(false, false, false, false);
    }

    protected User() {
    }

    private User(String phoneNumber, String passwordHash, String name, UserRole role) {
        this.phoneNumber = phoneNumber;
        this.passwordHash = passwordHash;
        this.name = name;
        this.role = role;
        this.status = UserStatus.ACTIVE;
    }

    public static User register(
            String phoneNumber, String passwordHash, String name, UserRole role) {
        return new User(phoneNumber, passwordHash, name, role);
    }

    public Long getId() {
        return id;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getName() {
        return name;
    }

    public UserRole getRole() {
        return role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }
}
