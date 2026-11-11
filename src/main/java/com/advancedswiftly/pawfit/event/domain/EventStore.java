package com.advancedswiftly.pawfit.event.domain;

import com.advancedswiftly.pawfit.auth.domain.Region;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "event_store")
public class EventStore {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "store_id")
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "event_id")
    private StampEvent event;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "region_id")
    private Region region;
    @Column(name = "name", nullable = false, length = 150)
    private String name;
    @Column(name = "latitude", precision = 10, scale = 7)
    private BigDecimal latitude;
    @Column(name = "longitude", precision = 10, scale = 7)
    private BigDecimal longitude;
    @Column(name = "approved", nullable = false)
    private boolean approved;
    @Column(name = "qr_token_hash", nullable = false, unique = true, length = 64)
    private String qrTokenHash;
    @Column(name = "qr_expires_at", nullable = false)
    private Instant qrExpiresAt;
    @Column(name = "qr_revoked", nullable = false)
    private boolean qrRevoked;
    protected EventStore() { }
    public boolean isQrUsableAt(Instant now) { return approved && !qrRevoked && now.isBefore(qrExpiresAt); }
    public Long getId() { return id; }
    public StampEvent getEvent() { return event; }
    public Region getRegion() { return region; }
    public String getName() { return name; }
    public BigDecimal getLatitude() { return latitude; }
    public BigDecimal getLongitude() { return longitude; }
    public String getQrTokenHash() { return qrTokenHash; }
}
