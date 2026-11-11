package com.advancedswiftly.pawfit.admin.application;

import com.advancedswiftly.pawfit.admin.api.dto.AdminEventDetailResponse;
import com.advancedswiftly.pawfit.admin.api.dto.DrawEntryActionRequest;
import com.advancedswiftly.pawfit.admin.api.dto.DrawEntryActionResponse;
import com.advancedswiftly.pawfit.admin.api.dto.DrawEntryListItem;
import com.advancedswiftly.pawfit.admin.api.dto.DrawEntryListResponse;
import com.advancedswiftly.pawfit.admin.domain.DrawEntryFulfillment;
import com.advancedswiftly.pawfit.admin.infrastructure.DrawEntryFulfillmentRepository;
import com.advancedswiftly.pawfit.event.domain.DrawEntryStatus;
import com.advancedswiftly.pawfit.event.domain.FulfillmentStatus;
import com.advancedswiftly.pawfit.global.error.BusinessException;
import com.advancedswiftly.pawfit.global.error.ErrorCode;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** 운영자의 이벤트 조회와 추첨 응모자·당첨 배송 처리. 이벤트 테이블은 이벤트 도메인 소유라 SQL로 다룬다. */
@Service
public class AdminEventService {

    public static final int PAGE_SIZE = 20;

    private final JdbcTemplate jdbcTemplate;
    private final DrawEntryFulfillmentRepository fulfillmentRepository;

    public AdminEventService(JdbcTemplate jdbcTemplate, DrawEntryFulfillmentRepository fulfillmentRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.fulfillmentRepository = fulfillmentRepository;
    }

    @Transactional(readOnly = true)
    public AdminEventDetailResponse getEvent(Long eventId) {
        List<AdminEventDetailResponse> rows = jdbcTemplate.query(
                "SELECT title, event_year, quarter, status, starts_on, ends_on, required_stamp_count, "
                        + "max_stamp_count, notice FROM stamp_event WHERE event_id = ?",
                (rs, rowNum) -> new AdminEventDetailResponse(
                        eventId,
                        rs.getString(1),
                        rs.getInt(2),
                        rs.getInt(3),
                        rs.getString(4),
                        rs.getDate(5).toLocalDate(),
                        rs.getDate(6).toLocalDate(),
                        rs.getInt(7),
                        rs.getInt(8),
                        rs.getString(9),
                        List.of(),
                        List.of(),
                        0,
                        0),
                eventId);
        if (rows.isEmpty()) {
            throw new BusinessException(ErrorCode.EVENT_NOT_FOUND);
        }
        AdminEventDetailResponse row = rows.get(0);
        List<AdminEventDetailResponse.Region> regions = jdbcTemplate.query(
                "SELECT r.region_id, r.name FROM event_region er JOIN region r ON r.region_id = er.region_id "
                        + "WHERE er.event_id = ? ORDER BY r.region_id",
                (rs, rowNum) -> new AdminEventDetailResponse.Region(rs.getLong(1), rs.getString(2)),
                eventId);
        List<AdminEventDetailResponse.Prize> prizes = jdbcTemplate.query(
                "SELECT prize_id, prize_rank, name, quantity FROM event_prize WHERE event_id = ? ORDER BY prize_rank",
                (rs, rowNum) -> new AdminEventDetailResponse.Prize(
                        rs.getLong(1), rs.getInt(2), rs.getString(3), rs.getInt(4)),
                eventId);
        long stores = count("SELECT COUNT(*) FROM event_store WHERE event_id = ? AND approved = TRUE", eventId);
        long entries = count("SELECT COUNT(*) FROM draw_entry WHERE event_id = ?", eventId);
        return new AdminEventDetailResponse(
                row.eventId(), row.title(), row.year(), row.quarter(), row.status(), row.startsOn(), row.endsOn(),
                row.requiredStampCount(), row.maxStampCount(), row.notice(), regions, prizes, stores, entries);
    }

    @Transactional(readOnly = true)
    public DrawEntryListResponse listDrawEntries(Long eventId, DrawEntryStatus status, int page) {
        requireEvent(eventId);
        StringBuilder sql = new StringBuilder(
                "SELECT d.draw_entry_id, d.user_id, d.status, d.fulfillment_status, "
                        + "(SELECT COUNT(*) FROM event_stamp s WHERE s.event_id = d.event_id AND s.user_id = d.user_id) "
                        + "FROM draw_entry d WHERE d.event_id = ?");
        List<Object> args = new ArrayList<>();
        args.add(eventId);
        if (status != null) {
            sql.append(" AND d.status = ?");
            args.add(status.name());
        }
        sql.append(" ORDER BY d.draw_entry_id DESC LIMIT ? OFFSET ?");
        args.add(PAGE_SIZE);
        args.add((page - 1) * PAGE_SIZE);
        List<DrawEntryListItem> items = jdbcTemplate.query(
                sql.toString(),
                (rs, rowNum) -> new DrawEntryListItem(
                        rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getLong(5), rs.getString(4)),
                args.toArray());
        return new DrawEntryListResponse(items);
    }

    /**
     * 당첨(WON) 응모의 배송 단계를 한 칸씩 진행한다.
     * CONTACTED(연락 대기→준비 중) → ADDRESS_CONFIRMED(주소 확인) → SHIPPED(발송, 운송장 필수) → DELIVERED(완료).
     * CANCEL_WIN은 당첨을 취소해 응모 상태를 CANCELED로 바꾼다.
     */
    @Transactional
    public DrawEntryActionResponse act(Long operatorId, Long drawEntryId, DrawEntryActionRequest request) {
        List<String[]> rows = jdbcTemplate.query(
                "SELECT status, fulfillment_status FROM draw_entry WHERE draw_entry_id = ?",
                (rs, rowNum) -> new String[] {rs.getString(1), rs.getString(2)},
                drawEntryId);
        if (rows.isEmpty()) {
            throw new BusinessException(ErrorCode.DRAW_ENTRY_NOT_FOUND);
        }
        DrawEntryStatus status = DrawEntryStatus.valueOf(rows.get(0)[0]);
        FulfillmentStatus fulfillment =
                rows.get(0)[1] == null ? FulfillmentStatus.CONTACT_PENDING : FulfillmentStatus.valueOf(rows.get(0)[1]);
        if (status != DrawEntryStatus.WON) {
            throw invalidState("당첨된 응모만 처리할 수 있습니다.");
        }
        DrawEntryFulfillment detail =
                fulfillmentRepository.findById(drawEntryId).orElseGet(() -> new DrawEntryFulfillment(drawEntryId));

        String newStatus = status.name();
        String newFulfillmentName = rows.get(0)[1]; // 당첨 취소는 배송 상태를 건드리지 않는다.
        switch (request.action()) {
            case CANCEL_WIN -> newStatus = DrawEntryStatus.CANCELED.name();
            case CONTACTED -> {
                requireFulfillment(fulfillment, FulfillmentStatus.CONTACT_PENDING);
                newFulfillmentName = FulfillmentStatus.PREPARING.name();
            }
            case ADDRESS_CONFIRMED -> {
                requireFulfillment(fulfillment, FulfillmentStatus.PREPARING);
                newFulfillmentName = fulfillment.name();
                detail.confirmAddress();
            }
            case SHIPPED -> {
                requireFulfillment(fulfillment, FulfillmentStatus.PREPARING);
                if (!detail.isAddressConfirmed()) {
                    throw invalidState("주소 확인 후에 발송할 수 있습니다.");
                }
                if (!StringUtils.hasText(request.trackingNumber())) {
                    throw new BusinessException(ErrorCode.INVALID_REQUEST, "운송장 번호는 필수입니다.");
                }
                detail.ship(request.trackingNumber().trim());
                newFulfillmentName = FulfillmentStatus.SHIPPED.name();
            }
            case DELIVERED -> {
                requireFulfillment(fulfillment, FulfillmentStatus.SHIPPED);
                newFulfillmentName = FulfillmentStatus.COMPLETED.name();
            }
        }
        detail.update(StringUtils.hasText(request.note()) ? request.note().trim() : null, operatorId);
        fulfillmentRepository.save(detail);
        jdbcTemplate.update(
                "UPDATE draw_entry SET status = ?, fulfillment_status = ? WHERE draw_entry_id = ?",
                newStatus, newFulfillmentName, drawEntryId);
        return new DrawEntryActionResponse(drawEntryId, newStatus, newFulfillmentName);
    }

    private void requireEvent(Long eventId) {
        if (count("SELECT COUNT(*) FROM stamp_event WHERE event_id = ?", eventId) == 0) {
            throw new BusinessException(ErrorCode.EVENT_NOT_FOUND);
        }
    }

    private static void requireFulfillment(FulfillmentStatus current, FulfillmentStatus expected) {
        if (current != expected) {
            throw invalidState("현재 배송 단계에서는 처리할 수 없습니다.");
        }
    }

    private static BusinessException invalidState(String message) {
        return new BusinessException(ErrorCode.INVALID_REQUEST_STATE, message);
    }

    private long count(String sql, Object... args) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class, args);
        return value == null ? 0L : value;
    }
}
