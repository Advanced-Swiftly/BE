package com.advancedswiftly.pawfit.event.domain;

import com.advancedswiftly.pawfit.auth.domain.User;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "visit")
public class Visit {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "visit_id")
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id")
    private User user;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "store_id")
    private EventStore store;
    @Column(name = "qr_token_hash", nullable = false, length = 64)
    private String qrTokenHash;
    @Column(name = "visited_at", nullable = false)
    private Instant visitedAt;
    @ElementCollection
    @CollectionTable(name = "visit_dog", joinColumns = @JoinColumn(name = "visit_id"))
    @Column(name = "dog_id", nullable = false)
    private Set<Long> dogIds = new LinkedHashSet<>();
    protected Visit() { }
    private Visit(User user, EventStore store, String hash, Instant at, List<Long> dogIds) {
        this.user = user; this.store = store; this.qrTokenHash = hash; this.visitedAt = at;
        this.dogIds.addAll(dogIds);
    }
    public static Visit create(User user, EventStore store, String hash, Instant at, List<Long> dogIds) {
        return new Visit(user, store, hash, at, dogIds);
    }
    public Long getId() { return id; }
    public EventStore getStore() { return store; }
    public Instant getVisitedAt() { return visitedAt; }
}
