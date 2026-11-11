package com.advancedswiftly.pawfit.admin.domain;

import com.advancedswiftly.pawfit.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** 이벤트 추첨 실행 기록. 이벤트당 한 번만 만들 수 있다. */
@Entity
@Table(name = "event_draw")
public class EventDraw extends BaseEntity {

    @Id
    @Column(name = "event_id")
    private Long eventId;

    @Column(name = "drawn_at", nullable = false)
    private Instant drawnAt;

    @Column(name = "seed", nullable = false, length = 100)
    private String seed;

    @Column(name = "winner_count", nullable = false)
    private int winnerCount;

    @Column(name = "executed_by")
    private Long executedBy;

    protected EventDraw() {
    }

    public EventDraw(Long eventId, Instant drawnAt, String seed, int winnerCount, Long executedBy) {
        this.eventId = eventId;
        this.drawnAt = drawnAt;
        this.seed = seed;
        this.winnerCount = winnerCount;
        this.executedBy = executedBy;
    }

    public Long getEventId() {
        return eventId;
    }

    public Instant getDrawnAt() {
        return drawnAt;
    }

    public int getWinnerCount() {
        return winnerCount;
    }
}
