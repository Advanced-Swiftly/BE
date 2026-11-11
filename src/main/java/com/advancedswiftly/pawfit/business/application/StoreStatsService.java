package com.advancedswiftly.pawfit.business.application;

import com.advancedswiftly.pawfit.business.api.dto.EventStatsResponse;
import com.advancedswiftly.pawfit.business.api.dto.VisitStatsPoint;
import com.advancedswiftly.pawfit.business.api.dto.VisitStatsResponse;
import com.advancedswiftly.pawfit.event.domain.StampEvent;
import com.advancedswiftly.pawfit.event.infrastructure.StampEventRepository;
import com.advancedswiftly.pawfit.global.error.BusinessException;
import com.advancedswiftly.pawfit.global.error.ErrorCode;
import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 방문·스탬프·응모 통계. 이벤트 영역의 visit, event_stamp, draw_entry를 업체 ID(store_id)로 집계한다.
 * 날짜 경계는 모두 UTC 기준이다.
 */
@Service
@Transactional(readOnly = true)
public class StoreStatsService {

    /** 조회 기간의 최대 일수. */
    static final int MAX_RANGE_DAYS = 366;

    public enum GroupBy {
        DAY,
        WEEK,
        MONTH
    }

    private final JdbcTemplate jdbcTemplate;
    private final StampEventRepository eventRepository;
    private final ManagedStoreChecker managedStoreChecker;

    public StoreStatsService(
            JdbcTemplate jdbcTemplate,
            StampEventRepository eventRepository,
            ManagedStoreChecker managedStoreChecker) {
        this.jdbcTemplate = jdbcTemplate;
        this.eventRepository = eventRepository;
        this.managedStoreChecker = managedStoreChecker;
    }

    public VisitStatsResponse getVisitStats(
            Long userId, Long storeId, LocalDate from, LocalDate to, GroupBy groupBy) {
        managedStoreChecker.requireManagedStore(userId, storeId);
        if (to.isBefore(from)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "조회 종료일은 시작일보다 빠를 수 없습니다.");
        }
        if (from.plusDays(MAX_RANGE_DAYS).isBefore(to)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "조회 기간은 최대 " + MAX_RANGE_DAYS + "일입니다.");
        }

        Timestamp start = startOf(from);
        Timestamp end = startOf(to.plusDays(1));

        List<Instant> visits = jdbcTemplate.query(
                "select visited_at from visit where store_id = ? and visited_at >= ? and visited_at < ?",
                (rs, rowNum) -> rs.getTimestamp("visited_at").toInstant(),
                storeId, start, end);
        long stampAwards = count(
                "select count(*) from event_stamp where store_id = ? and awarded_at >= ? and awarded_at < ?",
                storeId, start, end);

        Map<LocalDate, Long> buckets = new TreeMap<>();
        for (Instant visitedAt : visits) {
            LocalDate bucket = bucketOf(visitedAt.atZone(ZoneOffset.UTC).toLocalDate(), groupBy);
            buckets.merge(bucket, 1L, Long::sum);
        }
        List<VisitStatsPoint> series = buckets.entrySet().stream()
                .map(entry -> new VisitStatsPoint(entry.getKey(), entry.getValue()))
                .toList();
        return new VisitStatsResponse(visits.size(), stampAwards, series);
    }

    public EventStatsResponse getEventStats(Long userId, Long storeId, Long eventId) {
        managedStoreChecker.requireManagedStore(userId, storeId);
        StampEvent event = eventRepository
                .findById(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_NOT_FOUND));

        Timestamp start = startOf(event.getStartsOn());
        Timestamp end = startOf(event.getEndsOn().plusDays(1));

        long visits = count(
                "select count(*) from visit where store_id = ? and visited_at >= ? and visited_at < ?",
                storeId, start, end);
        long stampAwards = count(
                "select count(*) from event_stamp where event_id = ? and store_id = ?", eventId, storeId);
        long drawConversions = count(
                "select count(distinct d.user_id) from draw_entry d where d.event_id = ? and exists ("
                        + "select 1 from event_stamp s where s.event_id = d.event_id"
                        + " and s.user_id = d.user_id and s.store_id = ?)",
                eventId, storeId);
        return new EventStatsResponse(storeId, eventId, visits, stampAwards, drawConversions);
    }

    private long count(String sql, Object... args) {
        Long result = jdbcTemplate.queryForObject(sql, Long.class, args);
        return result == null ? 0L : result;
    }

    private Timestamp startOf(LocalDate date) {
        return Timestamp.from(date.atStartOfDay().toInstant(ZoneOffset.UTC));
    }

    private LocalDate bucketOf(LocalDate date, GroupBy groupBy) {
        return switch (groupBy) {
            case DAY -> date;
            case WEEK -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MONTH -> date.withDayOfMonth(1);
        };
    }
}
