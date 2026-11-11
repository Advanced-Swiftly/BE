package com.advancedswiftly.pawfit.business.domain;

import com.advancedswiftly.pawfit.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.DayOfWeek;
import java.time.LocalTime;

/** 업체의 요일별 영업시간. 휴무일이면 시간은 비어 있다. */
@Entity
@Table(name = "store_business_hours")
public class StoreBusinessHours extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "hours_id")
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 10)
    private DayOfWeek dayOfWeek;

    @Column(name = "open_time")
    private LocalTime openTime;

    @Column(name = "close_time")
    private LocalTime closeTime;

    @Column(name = "closed", nullable = false)
    private boolean closed;

    protected StoreBusinessHours() {
    }

    private StoreBusinessHours(
            Long storeId, DayOfWeek dayOfWeek, LocalTime openTime, LocalTime closeTime, boolean closed) {
        this.storeId = storeId;
        this.dayOfWeek = dayOfWeek;
        this.openTime = closed ? null : openTime;
        this.closeTime = closed ? null : closeTime;
        this.closed = closed;
    }

    public static StoreBusinessHours of(
            Long storeId, DayOfWeek dayOfWeek, LocalTime openTime, LocalTime closeTime, boolean closed) {
        return new StoreBusinessHours(storeId, dayOfWeek, openTime, closeTime, closed);
    }

    public void change(LocalTime openTime, LocalTime closeTime, boolean closed) {
        this.openTime = closed ? null : openTime;
        this.closeTime = closed ? null : closeTime;
        this.closed = closed;
    }

    public Long getId() {
        return id;
    }

    public Long getStoreId() {
        return storeId;
    }

    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }

    public LocalTime getOpenTime() {
        return openTime;
    }

    public LocalTime getCloseTime() {
        return closeTime;
    }

    public boolean isClosed() {
        return closed;
    }
}
