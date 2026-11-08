package com.advancedswiftly.pawfit.event.domain;

import com.advancedswiftly.pawfit.auth.domain.User;
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
import jakarta.persistence.Table;
import java.time.Instant;

@Entity @Table(name = "draw_entry")
public class DrawEntry {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "draw_entry_id") private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "event_id") private StampEvent event;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id") private User user;
    @Column(name = "phone_number", nullable = false, length = 20) private String phoneNumber;
    @Column(name = "address", nullable = false, length = 300) private String address;
    @Column(name = "privacy_agreed", nullable = false) private boolean privacyAgreed;
    @Enumerated(EnumType.STRING) @Column(name = "status", nullable = false, length = 30) private DrawEntryStatus status;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "prize_id") private EventPrize prize;
    @Enumerated(EnumType.STRING) @Column(name = "fulfillment_status", length = 30) private FulfillmentStatus fulfillmentStatus;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    protected DrawEntry() { }
    private DrawEntry(StampEvent event, User user, String phone, String address, Instant at) {
        this.event = event; this.user = user; this.phoneNumber = phone; this.address = address;
        this.privacyAgreed = true; this.status = DrawEntryStatus.ENTERED; this.createdAt = at;
    }
    public static DrawEntry create(StampEvent event, User user, String phone, String address, Instant at) {
        return new DrawEntry(event, user, phone, address, at);
    }
    public Long getId() { return id; }
    public DrawEntryStatus getStatus() { return status; }
    public EventPrize getPrize() { return prize; }
    public FulfillmentStatus getFulfillmentStatus() { return fulfillmentStatus; }
}
