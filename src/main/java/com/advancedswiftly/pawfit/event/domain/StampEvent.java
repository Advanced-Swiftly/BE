package com.advancedswiftly.pawfit.event.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "stamp_event")
public class StampEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id")
    private Long id;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "event_year", nullable = false)
    private int year;

    @Column(name = "quarter", nullable = false)
    private int quarter;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EventStatus status;

    @Column(name = "starts_on", nullable = false)
    private LocalDate startsOn;

    @Column(name = "ends_on", nullable = false)
    private LocalDate endsOn;

    @Column(name = "required_stamp_count", nullable = false)
    private int requiredStampCount;

    @Column(name = "max_stamp_count", nullable = false)
    private int maxStampCount;

    @Column(name = "notice", length = 2000)
    private String notice;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "event_region", joinColumns = @JoinColumn(name = "event_id"))
    @Column(name = "region_id", nullable = false)
    private Set<Long> regionIds = new LinkedHashSet<>();

    @OrderBy("rank ASC")
    @OneToMany(mappedBy = "event", fetch = FetchType.LAZY)
    private List<EventPrize> prizes;

    protected StampEvent() {
    }

    public boolean isOpenAt(LocalDate date) {
        return status == EventStatus.OPEN
                && !date.isBefore(startsOn)
                && !date.isAfter(endsOn);
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public int getYear() { return year; }
    public int getQuarter() { return quarter; }
    public EventStatus getStatus() { return status; }
    public LocalDate getStartsOn() { return startsOn; }
    public LocalDate getEndsOn() { return endsOn; }
    public int getRequiredStampCount() { return requiredStampCount; }
    public int getMaxStampCount() { return maxStampCount; }
    public String getNotice() { return notice; }
    public Set<Long> getRegionIds() { return Collections.unmodifiableSet(regionIds); }
    public List<EventPrize> getPrizes() { return List.copyOf(prizes); }
}
