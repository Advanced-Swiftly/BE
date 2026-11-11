package com.advancedswiftly.pawfit.admin.application;

import com.advancedswiftly.pawfit.admin.api.dto.AdminEventListItem;
import com.advancedswiftly.pawfit.admin.api.dto.AdminEventListResponse;
import com.advancedswiftly.pawfit.admin.api.dto.DrawResultResponse;
import com.advancedswiftly.pawfit.admin.api.dto.EventCreateRequest;
import com.advancedswiftly.pawfit.admin.api.dto.EventPrizeRequest;
import com.advancedswiftly.pawfit.admin.api.dto.EventStatusResponse;
import com.advancedswiftly.pawfit.admin.api.dto.EventUpdateRequest;
import com.advancedswiftly.pawfit.event.domain.EventStatus;
import com.advancedswiftly.pawfit.global.error.BusinessException;
import com.advancedswiftly.pawfit.global.error.ErrorCode;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 운영자의 이벤트 목록·생성·수정·삭제와 추첨 결과 조회. 이벤트 테이블은 이벤트 도메인 소유라 SQL로 다룬다. */
@Service
public class AdminEventManagementService {

    public static final int PAGE_SIZE = 20;

    private final JdbcTemplate jdbcTemplate;

    public AdminEventManagementService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public AdminEventListResponse list(Integer year, Integer quarter, EventStatus status, int page) {
        StringBuilder sql = new StringBuilder(
                "SELECT event_id, title, event_year, quarter, status, starts_on, ends_on FROM stamp_event WHERE 1 = 1");
        List<Object> args = new ArrayList<>();
        if (year != null) {
            sql.append(" AND event_year = ?");
            args.add(year);
        }
        if (quarter != null) {
            sql.append(" AND quarter = ?");
            args.add(quarter);
        }
        if (status != null) {
            sql.append(" AND status = ?");
            args.add(status.name());
        }
        sql.append(" ORDER BY event_year DESC, quarter DESC, event_id DESC LIMIT ? OFFSET ?");
        args.add(PAGE_SIZE);
        args.add((page - 1) * PAGE_SIZE);
        List<AdminEventListItem> items = jdbcTemplate.query(
                sql.toString(),
                (rs, rowNum) -> new AdminEventListItem(
                        rs.getLong(1),
                        rs.getString(2),
                        rs.getInt(3),
                        rs.getInt(4),
                        rs.getString(5),
                        rs.getDate(6).toLocalDate(),
                        rs.getDate(7).toLocalDate()),
                args.toArray());
        return new AdminEventListResponse(items);
    }

    /** 초안(DRAFT)으로 만든다. 같은 연도·분기 이벤트가 있으면 409. */
    @Transactional
    public EventStatusResponse create(EventCreateRequest request) {
        validatePeriod(request.startsOn(), request.endsOn());
        validateCounts(request.requiredStampCount(), request.maxStampCount());
        validatePrizes(request.prizes());
        Set<Long> regionIds = distinctRegions(request.regionIds());
        if (count("SELECT COUNT(*) FROM stamp_event WHERE event_year = ? AND quarter = ?",
                        request.year(), request.quarter())
                > 0) {
            throw new BusinessException(ErrorCode.DUPLICATE_EVENT_PERIOD);
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(
                connection -> {
                    var statement = connection.prepareStatement(
                            "INSERT INTO stamp_event (title, event_year, quarter, status, starts_on, ends_on, "
                                    + "required_stamp_count, max_stamp_count, notice) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                            new String[] {"event_id"});
                    statement.setString(1, request.title().trim());
                    statement.setInt(2, request.year());
                    statement.setInt(3, request.quarter());
                    statement.setString(4, EventStatus.DRAFT.name());
                    statement.setDate(5, Date.valueOf(request.startsOn()));
                    statement.setDate(6, Date.valueOf(request.endsOn()));
                    statement.setInt(7, request.requiredStampCount());
                    statement.setInt(8, request.maxStampCount());
                    statement.setString(9, request.notice());
                    return statement;
                },
                keyHolder);
        Long eventId = keyHolder.getKey().longValue();
        insertRegions(eventId, regionIds);
        insertPrizes(eventId, request.prizes());
        return new EventStatusResponse(eventId, EventStatus.DRAFT.name());
    }

    /** 초안만 고칠 수 있다. regionIds·prizes는 전달하면 전체 교체, status OPEN은 공개. */
    @Transactional
    public EventStatusResponse update(Long eventId, EventUpdateRequest request) {
        List<Object[]> rows = jdbcTemplate.query(
                "SELECT status, title, starts_on, ends_on, required_stamp_count, max_stamp_count, notice "
                        + "FROM stamp_event WHERE event_id = ?",
                (rs, rowNum) -> new Object[] {
                    rs.getString(1), rs.getString(2), rs.getDate(3).toLocalDate(), rs.getDate(4).toLocalDate(),
                    rs.getInt(5), rs.getInt(6), rs.getString(7)
                },
                eventId);
        if (rows.isEmpty()) {
            throw new BusinessException(ErrorCode.EVENT_NOT_FOUND);
        }
        Object[] row = rows.get(0);
        if (!EventStatus.DRAFT.name().equals(row[0])) {
            throw new BusinessException(ErrorCode.EVENT_NOT_EDITABLE);
        }
        boolean publish = false;
        if (request.status() != null) {
            if (!EventStatus.OPEN.name().equals(request.status())) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "status는 OPEN만 지정할 수 있습니다.");
            }
            publish = true;
        }
        String title = request.title() != null ? request.title().trim() : (String) row[1];
        LocalDate startsOn = request.startsOn() != null ? request.startsOn() : (LocalDate) row[2];
        LocalDate endsOn = request.endsOn() != null ? request.endsOn() : (LocalDate) row[3];
        int required = request.requiredStampCount() != null ? request.requiredStampCount() : (Integer) row[4];
        int max = request.maxStampCount() != null ? request.maxStampCount() : (Integer) row[5];
        String notice = request.notice() != null ? request.notice() : (String) row[6];
        validatePeriod(startsOn, endsOn);
        validateCounts(required, max);
        Set<Long> regionIds = request.regionIds() == null ? null : distinctRegions(request.regionIds());
        if (request.prizes() != null) {
            validatePrizes(request.prizes());
        }

        if (publish) {
            long regionCount = regionIds != null ? regionIds.size() : count(
                    "SELECT COUNT(*) FROM event_region WHERE event_id = ?", eventId);
            long prizeCount = request.prizes() != null ? request.prizes().size() : count(
                    "SELECT COUNT(*) FROM event_prize WHERE event_id = ?", eventId);
            if (regionCount == 0 || prizeCount == 0) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "공개하려면 적용 지역과 경품이 필요합니다.");
            }
        }

        jdbcTemplate.update(
                "UPDATE stamp_event SET title = ?, starts_on = ?, ends_on = ?, required_stamp_count = ?, "
                        + "max_stamp_count = ?, notice = ?, status = ? WHERE event_id = ?",
                title, Date.valueOf(startsOn), Date.valueOf(endsOn), required, max, notice,
                publish ? EventStatus.OPEN.name() : EventStatus.DRAFT.name(), eventId);
        if (regionIds != null) {
            jdbcTemplate.update("DELETE FROM event_region WHERE event_id = ?", eventId);
            insertRegions(eventId, regionIds);
        }
        if (request.prizes() != null) {
            jdbcTemplate.update("DELETE FROM event_prize WHERE event_id = ?", eventId);
            insertPrizes(eventId, request.prizes());
        }
        return new EventStatusResponse(eventId, publish ? EventStatus.OPEN.name() : EventStatus.DRAFT.name());
    }

    /** 초안이고 참여 업체·응모가 없을 때만 삭제한다. */
    @Transactional
    public void delete(Long eventId) {
        List<String> status = jdbcTemplate.query(
                "SELECT status FROM stamp_event WHERE event_id = ?", (rs, rowNum) -> rs.getString(1), eventId);
        if (status.isEmpty()) {
            throw new BusinessException(ErrorCode.EVENT_NOT_FOUND);
        }
        if (!EventStatus.DRAFT.name().equals(status.get(0))
                || count("SELECT COUNT(*) FROM event_store WHERE event_id = ?", eventId) > 0
                || count("SELECT COUNT(*) FROM draw_entry WHERE event_id = ?", eventId) > 0) {
            throw new BusinessException(ErrorCode.EVENT_NOT_DELETABLE);
        }
        jdbcTemplate.update("DELETE FROM event_region WHERE event_id = ?", eventId);
        jdbcTemplate.update("DELETE FROM event_prize WHERE event_id = ?", eventId);
        jdbcTemplate.update("DELETE FROM stamp_event WHERE event_id = ?", eventId);
    }

    /** 당첨(WON)과 당첨 취소(CANCELED) 응모를 경품 순위순으로 돌려준다. */
    @Transactional(readOnly = true)
    public DrawResultResponse drawResult(Long eventId) {
        if (count("SELECT COUNT(*) FROM stamp_event WHERE event_id = ?", eventId) == 0) {
            throw new BusinessException(ErrorCode.EVENT_NOT_FOUND);
        }
        List<DrawResultResponse.Winner> winners = jdbcTemplate.query(
                "SELECT d.draw_entry_id, d.user_id, p.prize_rank, p.name, d.status, d.fulfillment_status, "
                        + "f.tracking_number FROM draw_entry d "
                        + "LEFT JOIN event_prize p ON p.prize_id = d.prize_id "
                        + "LEFT JOIN draw_entry_fulfillment f ON f.draw_entry_id = d.draw_entry_id "
                        + "WHERE d.event_id = ? AND d.status IN ('WON', 'CANCELED') "
                        + "ORDER BY p.prize_rank, d.draw_entry_id",
                (rs, rowNum) -> new DrawResultResponse.Winner(
                        rs.getLong(1),
                        rs.getLong(2),
                        rs.getObject(3, Integer.class),
                        rs.getString(4),
                        rs.getString(5),
                        rs.getString(6),
                        rs.getString(7)),
                eventId);
        return new DrawResultResponse(eventId, winners);
    }

    private void validatePeriod(LocalDate startsOn, LocalDate endsOn) {
        if (endsOn.isBefore(startsOn)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "종료일은 시작일보다 빠를 수 없습니다.");
        }
    }

    private void validateCounts(int required, int max) {
        if (max < required) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "최대 스탬프 수는 필요 스탬프 수 이상이어야 합니다.");
        }
    }

    private void validatePrizes(List<EventPrizeRequest> prizes) {
        Set<Integer> ranks = new HashSet<>();
        for (EventPrizeRequest prize : prizes) {
            if (!ranks.add(prize.rank())) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "경품 순위가 중복되었습니다.");
            }
        }
    }

    private Set<Long> distinctRegions(List<Long> regionIds) {
        Set<Long> distinct = new HashSet<>(regionIds);
        for (Long regionId : distinct) {
            if (count("SELECT COUNT(*) FROM region WHERE region_id = ?", regionId) == 0) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "존재하지 않는 지역이 포함되어 있습니다.");
            }
        }
        return distinct;
    }

    private void insertRegions(Long eventId, Set<Long> regionIds) {
        for (Long regionId : regionIds) {
            jdbcTemplate.update("INSERT INTO event_region (event_id, region_id) VALUES (?, ?)", eventId, regionId);
        }
    }

    private void insertPrizes(Long eventId, List<EventPrizeRequest> prizes) {
        for (EventPrizeRequest prize : prizes) {
            jdbcTemplate.update(
                    "INSERT INTO event_prize (event_id, prize_rank, name, quantity) VALUES (?, ?, ?, ?)",
                    eventId, prize.rank(), prize.name().trim(), prize.quantity());
        }
    }

    private long count(String sql, Object... args) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class, args);
        return value == null ? 0L : value;
    }
}
