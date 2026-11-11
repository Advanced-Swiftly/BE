package com.advancedswiftly.pawfit.admin.application;

import com.advancedswiftly.pawfit.admin.api.dto.AdminAdApplicationListItem;
import com.advancedswiftly.pawfit.admin.api.dto.AdminAdApplicationListResponse;
import com.advancedswiftly.pawfit.admin.api.dto.AdminEventApplicationListItem;
import com.advancedswiftly.pawfit.admin.api.dto.AdminEventApplicationListResponse;
import com.advancedswiftly.pawfit.business.domain.AdType;
import com.advancedswiftly.pawfit.business.domain.ApplicationStatus;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 운영자의 광고·이벤트 참여 신청 목록 조회. */
@Service
public class AdminListService {

    public static final int PAGE_SIZE = 20;

    private final JdbcTemplate jdbcTemplate;

    public AdminListService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** from/to는 광고 노출 기간과 겹치는 신청만 남긴다. */
    @Transactional(readOnly = true)
    public AdminAdApplicationListResponse listAds(
            ApplicationStatus status, AdType adType, LocalDate from, LocalDate to, int page) {
        StringBuilder sql = new StringBuilder(
                "SELECT a.application_id, a.store_id, s.name, a.ad_type, a.start_date, a.end_date, a.status, "
                        + "a.created_at FROM ad_application a LEFT JOIN store s ON s.store_id = a.store_id WHERE 1 = 1");
        List<Object> args = new ArrayList<>();
        if (status != null) {
            sql.append(" AND a.status = ?");
            args.add(status.name());
        }
        if (adType != null) {
            sql.append(" AND a.ad_type = ?");
            args.add(adType.name());
        }
        if (from != null) {
            sql.append(" AND a.end_date >= ?");
            args.add(Date.valueOf(from));
        }
        if (to != null) {
            sql.append(" AND a.start_date <= ?");
            args.add(Date.valueOf(to));
        }
        sql.append(" ORDER BY a.application_id DESC LIMIT ? OFFSET ?");
        args.add(PAGE_SIZE);
        args.add((page - 1) * PAGE_SIZE);
        List<AdminAdApplicationListItem> items = jdbcTemplate.query(
                sql.toString(),
                (rs, rowNum) -> new AdminAdApplicationListItem(
                        rs.getLong(1),
                        rs.getLong(2),
                        rs.getString(3),
                        AdType.valueOf(rs.getString(4)),
                        rs.getDate(5).toLocalDate(),
                        rs.getDate(6).toLocalDate(),
                        ApplicationStatus.valueOf(rs.getString(7)),
                        rs.getTimestamp(8).toInstant()),
                args.toArray());
        return new AdminAdApplicationListResponse(items);
    }

    @Transactional(readOnly = true)
    public AdminEventApplicationListResponse listEventApplications(
            Long eventId, Long regionId, ApplicationStatus status, int page) {
        StringBuilder sql = new StringBuilder(
                "SELECT a.application_id, a.store_id, s.name, a.event_id, a.status, a.created_at "
                        + "FROM event_application a LEFT JOIN store s ON s.store_id = a.store_id WHERE 1 = 1");
        List<Object> args = new ArrayList<>();
        if (eventId != null) {
            sql.append(" AND a.event_id = ?");
            args.add(eventId);
        }
        if (regionId != null) {
            sql.append(" AND EXISTS (SELECT 1 FROM event_application_region r "
                    + "WHERE r.application_id = a.application_id AND r.region_id = ?)");
            args.add(regionId);
        }
        if (status != null) {
            sql.append(" AND a.status = ?");
            args.add(status.name());
        }
        sql.append(" ORDER BY a.application_id DESC LIMIT ? OFFSET ?");
        args.add(PAGE_SIZE);
        args.add((page - 1) * PAGE_SIZE);
        List<AdminEventApplicationListItem> items = jdbcTemplate.query(
                sql.toString(),
                (rs, rowNum) -> new AdminEventApplicationListItem(
                        rs.getLong(1),
                        rs.getLong(2),
                        rs.getString(3),
                        rs.getLong(4),
                        List.of(),
                        ApplicationStatus.valueOf(rs.getString(5)),
                        rs.getTimestamp(6).toInstant()),
                args.toArray());
        List<AdminEventApplicationListItem> withRegions = new ArrayList<>();
        for (AdminEventApplicationListItem item : items) {
            List<Long> regionIds = jdbcTemplate.query(
                    "SELECT region_id FROM event_application_region WHERE application_id = ? ORDER BY region_id",
                    (rs, rowNum) -> rs.getLong(1),
                    item.applicationId());
            withRegions.add(new AdminEventApplicationListItem(
                    item.applicationId(), item.storeId(), item.storeName(), item.eventId(), regionIds,
                    item.status(), item.createdAt()));
        }
        return new AdminEventApplicationListResponse(withRegions);
    }
}
