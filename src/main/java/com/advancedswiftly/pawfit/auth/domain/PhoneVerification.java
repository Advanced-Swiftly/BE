package com.advancedswiftly.pawfit.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "phone_verification")
public class PhoneVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "verification_id")
    private Long id;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 30)
    private PhoneVerificationPurpose purpose;

    @Column(name = "code_hash", nullable = false, length = 100)
    private String codeHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "verification_token_hash", length = 64)
    private String verificationTokenHash;

    @Column(name = "token_expires_at")
    private Instant tokenExpiresAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PhoneVerification() {
    }

    private PhoneVerification(
            String phoneNumber,
            PhoneVerificationPurpose purpose,
            String codeHash,
            Instant expiresAt) {
        this.phoneNumber = phoneNumber;
        this.purpose = purpose;
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
    }

    public static PhoneVerification start(
            String phoneNumber,
            PhoneVerificationPurpose purpose,
            String codeHash,
            Instant expiresAt) {
        return new PhoneVerification(phoneNumber, purpose, codeHash, expiresAt);
    }

    public boolean canVerifyAt(Instant now, int maxAttempts) {
        return verifiedAt == null && expiresAt.isAfter(now) && attemptCount < maxAttempts;
    }

    public void recordFailedAttempt() {
        attemptCount++;
    }

    public void verify(String tokenHash, Instant verifiedAt, Instant tokenExpiresAt) {
        this.verificationTokenHash = tokenHash;
        this.verifiedAt = verifiedAt;
        this.tokenExpiresAt = tokenExpiresAt;
    }

    public boolean canConsumeAt(Instant now, PhoneVerificationPurpose expectedPurpose) {
        return purpose == expectedPurpose
                && verifiedAt != null
                && consumedAt == null
                && tokenExpiresAt != null
                && tokenExpiresAt.isAfter(now);
    }

    public void consume(Instant now) {
        consumedAt = now;
    }

    public Long getId() {
        return id;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getCodeHash() {
        return codeHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
