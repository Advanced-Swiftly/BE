package com.advancedswiftly.pawfit.event.domain;

import com.advancedswiftly.pawfit.auth.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity @Table(name = "event_stamp")
public class EventStamp {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "stamp_id") private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "event_id") private StampEvent event;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id") private User user;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "store_id") private EventStore store;
    @Column(name = "awarded_at", nullable = false) private Instant awardedAt;
    protected EventStamp() { }
    private EventStamp(StampEvent event, User user, EventStore store, Instant at) {
        this.event = event; this.user = user; this.store = store; this.awardedAt = at;
    }
    public static EventStamp create(StampEvent event, User user, EventStore store, Instant at) {
        return new EventStamp(event, user, store, at);
    }
    public EventStore getStore() { return store; }
}
