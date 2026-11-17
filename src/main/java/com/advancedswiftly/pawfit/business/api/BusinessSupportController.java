package com.advancedswiftly.pawfit.business.api;

import com.advancedswiftly.pawfit.business.application.ManagedStoreChecker;
import com.advancedswiftly.pawfit.business.application.StoreStatsService;
import com.advancedswiftly.pawfit.business.domain.EntryConditions;
import com.advancedswiftly.pawfit.business.infrastructure.StorePolicyRepository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/business")
@PreAuthorize("hasRole('BUSINESS')")
public class BusinessSupportController {
    private final JdbcTemplate db;
    private final com.advancedswiftly.pawfit.place.PlaceService places;
    private final ManagedStoreChecker managed;
    private final StoreStatsService stats;
    private final StorePolicyRepository policies;

    public BusinessSupportController(
            JdbcTemplate db,
            ManagedStoreChecker managed,
            StoreStatsService stats,
            StorePolicyRepository policies,
            com.advancedswiftly.pawfit.place.PlaceService places) {
        this.places = places;
        this.db = db;
        this.managed = managed;
        this.stats = stats;
        this.policies = policies;
    }

    @PostMapping("/stores/{storeId}/policy-preview")
    public com.advancedswiftly.pawfit.place.PlaceService.PolicyPreview preview(
            Authentication auth,
            @PathVariable Long storeId,
            @jakarta.validation.Valid @RequestBody
                    com.advancedswiftly.pawfit.business.api.dto.RequestedPolicy policy) {
        managed.requireManagedStore(Long.valueOf(auth.getName()), storeId);
        return places.preview(storeId, policy);
    }

    @GetMapping("/faqs")
    public Map<String, Object> faqs() {
        return Map.of(
                "faqs",
                db.query(
                        "select question,answer from business_faq where visible=true order by"
                                + " display_order,faq_id",
                        (r, n) -> Map.of("question", r.getString(1), "answer", r.getString(2))));
    }

    @GetMapping("/stores/{storeId}/dashboard")
    public Dashboard dashboard(Authentication auth, @PathVariable Long storeId) {
        long userId = Long.parseLong(auth.getName());
        managed.requireManagedStore(userId, storeId);
        var today = EntryConditions.today();
        var month =
                stats.getVisitStats(
                        userId,
                        storeId,
                        today.withDayOfMonth(1),
                        today,
                        StoreStatsService.GroupBy.DAY);
        Long pending =
                db.queryForObject(
                        "select count(*) from condition_request where store_id=? and status in"
                                + " ('RECEIVED','IN_REVIEW','NEEDS_SUPPLEMENT')",
                        Long.class,
                        storeId);
        var checked =
                policies.findActive(storeId)
                        .map(
                                p ->
                                        p.getCreatedAt()
                                                .atZone(java.time.ZoneId.of("Asia/Seoul"))
                                                .toLocalDate())
                        .orElse(null);
        return new Dashboard(storeId, month.totalVisits(), month.stampAwards(), pending, checked);
    }

    public record Dashboard(
            Long placeId,
            long qrVisitCount,
            long stampCount,
            long inProgressRequestCount,
            java.time.LocalDate conditionCheckedAt) {}
}
