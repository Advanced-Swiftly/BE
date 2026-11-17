package com.advancedswiftly.pawfit.business.api;

import com.advancedswiftly.pawfit.business.application.*;
import com.advancedswiftly.pawfit.business.domain.EntryConditions;
import com.advancedswiftly.pawfit.event.infrastructure.StampEventRepository;
import com.advancedswiftly.pawfit.global.error.*;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/business/events")
@PreAuthorize("hasRole('BUSINESS')")
@Transactional(readOnly = true)
public class BusinessEventController {
    private final JdbcTemplate db;
    private final StampEventRepository events;
    private final ManagedStoreChecker managed;
    private final EventEligibilityService eligibility;

    public BusinessEventController(
            JdbcTemplate db,
            StampEventRepository events,
            ManagedStoreChecker managed,
            EventEligibilityService eligibility) {
        this.db = db;
        this.events = events;
        this.managed = managed;
        this.eligibility = eligibility;
    }

    @GetMapping
    public Map<String, Object> list(Authentication auth, @RequestParam Long placeId) {
        long user = Long.parseLong(auth.getName());
        managed.requireManagedStore(user, placeId);
        var recruiting = new ArrayList<Map<String, Object>>();
        var participating = new ArrayList<Map<String, Object>>();
        for (Long id :
                db.queryForList(
                        "select event_id from stamp_event where status<>'DRAFT' order by starts_on"
                                + " desc,event_id desc",
                        Long.class)) {
            var detail = detail(auth, id, placeId);
            if (Boolean.TRUE.equals(detail.get("recruitmentOpen"))
                    && detail.get("participation") == null) recruiting.add(detail);
            if (detail.get("participation") != null) participating.add(detail);
        }
        return Map.of(
                "placeId",
                placeId.toString(),
                "serverTime",
                java.time.Instant.now().toString(),
                "recruiting",
                recruiting,
                "participating",
                participating);
    }

    @GetMapping("/{eventId}")
    public Map<String, Object> detail(
            Authentication auth, @PathVariable Long eventId, @RequestParam Long placeId) {
        long user = Long.parseLong(auth.getName());
        managed.requireManagedStore(user, placeId);
        var event =
                events.findDetailById(eventId)
                        .filter(e -> !e.getStatus().name().equals("DRAFT"))
                        .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_NOT_FOUND));
        var check = eligibility.get(user, eventId, placeId);
        var today = EntryConditions.today();
        String phase =
                today.isAfter(event.getEndsOn()) || !event.getStatus().name().equals("OPEN")
                        ? "ENDED"
                        : today.isBefore(event.getStartsOn())
                                ? (check.recruitmentOpen() ? "RECRUITING" : "UPCOMING")
                                : "IN_PROGRESS";
        var result = new LinkedHashMap<String, Object>();
        result.put("eventId", eventId.toString());
        result.put("periodId", event.getYear() + "Q" + event.getQuarter());
        result.put("title", event.getTitle());
        result.put("phase", phase);
        result.put(
                "period",
                Map.of(
                        "startDate",
                        event.getStartsOn().toString(),
                        "endDate",
                        event.getEndsOn().toString()));
        result.put(
                "recruitment",
                Map.of(
                        "startAt",
                        event.getRecruitmentStartsOn() + "T00:00:00+09:00",
                        "endAt",
                        event.getRecruitmentEndsOn().plusDays(1) + "T00:00:00+09:00"));
        result.put("recruitmentOpen", check.recruitmentOpen());
        var options =
                db.query(
                        "select r.region_id,r.name from region r join event_region e on"
                            + " e.region_id=r.region_id where e.event_id=? order by r.region_id",
                        (r, n) -> Map.of("id", Long.toString(r.getLong(1)), "name", r.getString(2)),
                        eventId);
        result.put("regions", options.stream().map(r -> r.get("name")).toList());
        result.put("regionOptions", options);
        result.put(
                "prizes",
                event.getPrizes().stream()
                        .map(
                                p ->
                                        Map.of(
                                                "name",
                                                p.getName(),
                                                "quantity",
                                                p.getQuantity(),
                                                "rank",
                                                p.getRank()))
                        .toList());
        result.put("description", event.getNotice());
        result.put(
                "conditions",
                List.of("REGION", "BUSINESS_VERIFIED", "QR_ACTIVE", "CONDITION_CHECKED_RECENT"));
        String storeName =
                db.queryForObject("select name from store where store_id=?", String.class, placeId);
        result.put(
                "applicablePlaces",
                List.of(Map.of("placeId", placeId.toString(), "name", storeName)));
        result.put("placeId", placeId.toString());
        result.put("placeInRegion", check.checks().get(0).passed());
        Object app = null;
        Object participation = null;
        if (check.existingApplication() != null) {
            var existing = check.existingApplication();
            app =
                    Map.of(
                            "applicationId",
                            existing.applicationId(),
                            "status",
                            existing.status().name(),
                            "displayStatus",
                            existing.status().name());
            if (existing.status().name().equals("APPROVED")) {
                String approvedAt =
                        db.queryForObject(
                                "select cast(reviewed_at as varchar) from event_application where"
                                        + " application_id=?",
                                String.class,
                                Long.valueOf(existing.applicationId()));
                participation =
                        Map.of(
                                "participationId",
                                existing.applicationId(),
                                "approvedAt",
                                approvedAt == null ? "" : approvedAt);
                result.put("participationId", existing.applicationId());
                result.put("approvedAt", approvedAt == null ? "" : approvedAt);
            }
        }
        result.put("myApplication", app);
        result.put("participation", participation);
        result.put("canApply", check.canApply());
        result.put(
                "applyBlockedReason",
                !check.recruitmentOpen()
                        ? "NOT_RECRUITING"
                        : app != null
                                ? "ALREADY_APPLIED"
                                : !check.allPassed() ? "CHECK_FAILED" : null);
        return result;
    }
}
