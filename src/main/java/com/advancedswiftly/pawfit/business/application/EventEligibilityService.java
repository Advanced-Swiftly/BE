package com.advancedswiftly.pawfit.business.application;

import com.advancedswiftly.pawfit.business.api.dto.EventEligibilityResponse;
import com.advancedswiftly.pawfit.business.api.dto.EventEligibilityResponse.Check;
import com.advancedswiftly.pawfit.business.api.dto.EventEligibilityResponse.Code;
import com.advancedswiftly.pawfit.business.api.dto.EventEligibilityResponse.Detail;
import com.advancedswiftly.pawfit.business.api.dto.EventEligibilityResponse.ExistingApplication;
import com.advancedswiftly.pawfit.business.api.dto.EventEligibilityResponse.Reason;
import com.advancedswiftly.pawfit.business.domain.ApplicationStatus;
import com.advancedswiftly.pawfit.business.domain.QrStatus;
import com.advancedswiftly.pawfit.business.domain.Store;
import com.advancedswiftly.pawfit.business.domain.StoreVerificationStatus;
import com.advancedswiftly.pawfit.business.infrastructure.EventApplicationRepository;
import com.advancedswiftly.pawfit.business.infrastructure.StorePolicyRepository;
import com.advancedswiftly.pawfit.business.infrastructure.StoreQrRepository;
import com.advancedswiftly.pawfit.business.infrastructure.StoreRepository;
import com.advancedswiftly.pawfit.event.application.EventRecruitmentService;
import com.advancedswiftly.pawfit.global.error.BusinessException;
import com.advancedswiftly.pawfit.global.error.ErrorCode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventEligibilityService {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final List<ApplicationStatus> REAPPLY_ALLOWED =
            List.of(ApplicationStatus.REJECTED, ApplicationStatus.CANCELED);

    private final ManagedStoreChecker managedStores;
    private final StoreRepository stores;
    private final StorePolicyRepository policies;
    private final StoreQrRepository qrs;
    private final EventApplicationRepository applications;
    private final EventRecruitmentService recruitment;
    private final Clock clock;

    @Autowired
    public EventEligibilityService(ManagedStoreChecker managedStores, StoreRepository stores,
            StorePolicyRepository policies, StoreQrRepository qrs, EventApplicationRepository applications,
            EventRecruitmentService recruitment) {
        this(managedStores, stores, policies, qrs, applications, recruitment, Clock.systemUTC());
    }

    EventEligibilityService(ManagedStoreChecker managedStores, StoreRepository stores,
            StorePolicyRepository policies, StoreQrRepository qrs, EventApplicationRepository applications,
            EventRecruitmentService recruitment, Clock clock) {
        this.managedStores = managedStores;
        this.stores = stores;
        this.policies = policies;
        this.qrs = qrs;
        this.applications = applications;
        this.recruitment = recruitment;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public EventEligibilityResponse get(Long userId, Long eventId, Long placeId) {
        managedStores.requireManagedStore(userId, placeId);
        Store store = stores.findById(placeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        LocalDate today = LocalDate.ofInstant(clock.instant(), SEOUL);
        EventRecruitmentService.Recruitment event = recruitment.get(eventId, today);
        ExistingApplication existing = applications
                .findFirstByStoreIdAndEventIdAndStatusNotInOrderByIdDesc(placeId, eventId, REAPPLY_ALLOWED)
                .map(application -> new ExistingApplication(application.getId().toString(), application.getStatus()))
                .orElse(null);
        boolean activeQr = qrs.findFirstByStoreIdOrderByIdDesc(placeId)
                .filter(qr -> qr.getStatus() == QrStatus.ACTIVE && qr.getQrTokenHash() != null)
                .isPresent();
        LocalDate checkedDate = policies.findFirstByStoreIdOrderByVersionDesc(placeId)
                .map(policy -> policy.getCreatedAt())
                .map(instant -> LocalDate.ofInstant(instant, SEOUL)).orElse(null);
        Long days = checkedDate == null ? null : ChronoUnit.DAYS.between(checkedDate, today);
        Reason reason = days == null ? Reason.NO_DATE
                : days < 0 ? Reason.FUTURE_DATE : days > 30 ? Reason.OVER_30_DAYS : null;
        List<Check> checks = List.of(
                new Check(Code.REGION, store.getRegionId() != null && event.regionIds().contains(store.getRegionId()), null),
                new Check(Code.BUSINESS_VERIFIED, store.getVerificationStatus() == StoreVerificationStatus.VERIFIED, null),
                new Check(Code.QR_ACTIVE, activeQr, null),
                new Check(Code.CONDITION_CHECKED_RECENT, reason == null, new Detail(checkedDate, days, reason)));
        boolean allPassed = checks.stream().allMatch(Check::passed);
        return new EventEligibilityResponse(eventId.toString(), placeId.toString(), today, event.open(), existing,
                checks, allPassed, allPassed && event.open() && existing == null);
    }
}
