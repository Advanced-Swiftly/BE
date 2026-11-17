package com.advancedswiftly.pawfit.chat;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chat_profile_action")
public class ChatProfileAction {
    @Id
    @Column(name = "action_id")
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "dog_id", nullable = false)
    private Long dogId;

    @Column(name = "equipment_code", nullable = false, length = 50)
    private String equipmentCode;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "applied", nullable = false)
    private boolean applied;

    protected ChatProfileAction() {}

    public ChatProfileAction(Long userId, Long sessionId, Long dogId, String code) {
        id = UUID.randomUUID();
        this.userId = userId;
        this.sessionId = sessionId;
        this.dogId = dogId;
        equipmentCode = code;
        expiresAt = Instant.now().plusSeconds(600);
    }

    public UUID getId() {
        return id;
    }

    public Long getDogId() {
        return dogId;
    }

    public String getEquipmentCode() {
        return equipmentCode;
    }

    public boolean matches(Long userId, Long sessionId) {
        return this.userId.equals(userId) && this.sessionId.equals(sessionId);
    }

    public boolean isApplied() {
        return applied;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public void apply() {
        applied = true;
    }
}
