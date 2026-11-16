package com.advancedswiftly.pawfit.business.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.advancedswiftly.pawfit.business.api.dto.EventEligibilityResponse;
import com.advancedswiftly.pawfit.business.domain.ApplicationStatus;
import com.advancedswiftly.pawfit.business.domain.EventApplication;
import com.advancedswiftly.pawfit.business.domain.QrStatus;
import com.advancedswiftly.pawfit.business.domain.Store;
import com.advancedswiftly.pawfit.business.domain.StorePolicy;
import com.advancedswiftly.pawfit.business.domain.StoreQr;
import com.advancedswiftly.pawfit.business.domain.StoreVerificationStatus;
import com.advancedswiftly.pawfit.business.infrastructure.EventApplicationRepository;
import com.advancedswiftly.pawfit.business.infrastructure.StorePolicyRepository;
import com.advancedswiftly.pawfit.business.infrastructure.StoreQrRepository;
import com.advancedswiftly.pawfit.business.infrastructure.StoreRepository;
import com.advancedswiftly.pawfit.event.application.EventRecruitmentService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class EventEligibilityServiceTest {

    private final StoreRepository stores = mock(StoreRepository.class);
    private final StorePolicyRepository policies = mock(StorePolicyRepository.class);
    private final StoreQrRepository qrs = mock(StoreQrRepository.class);
    private final EventApplicationRepository applications = mock(EventApplicationRepository.class);
    private final EventRecruitmentService recruitment = mock(EventRecruitmentService.class);
    private final Store store = mock(Store.class);
    private final StorePolicy policy = mock(StorePolicy.class);
    private final StoreQr qr = mock(StoreQr.class);
    private EventEligibilityService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-16T15:30:00Z"), ZoneOffset.UTC);
        service = new EventEligibilityService(mock(ManagedStoreChecker.class), stores, policies, qrs,
                applications, recruitment, clock);
        when(stores.findById(2L)).thenReturn(Optional.of(store));
        when(store.getRegionId()).thenReturn(11L);
        when(store.getVerificationStatus()).thenReturn(StoreVerificationStatus.VERIFIED);
        when(recruitment.get(eq(3L), any())).thenReturn(new EventRecruitmentService.Recruitment(Set.of(11L), true));
        when(policies.findFirstByStoreIdOrderByVersionDesc(2L)).thenReturn(Optional.of(policy));
        when(policy.getCreatedAt()).thenReturn(clock.instant());
        when(qrs.findFirstByStoreIdOrderByIdDesc(2L)).thenReturn(Optional.of(qr));
        when(qr.getStatus()).thenReturn(QrStatus.ACTIVE);
        when(qr.getQrTokenHash()).thenReturn("valid-token-hash");
    }

    @ParameterizedTest
    @CsvSource({"2026-11-16T15:00:00Z,0,true,", "2026-10-17T15:00:00Z,30,true,",
            "2026-10-16T15:00:00Z,31,false,OVER_30_DAYS", "2026-11-17T15:00:00Z,-1,false,FUTURE_DATE"})
    void evaluatesThirtyDayBoundaryUsingKoreanDate(String instant, long days, boolean passed, String reason) {
        when(policy.getCreatedAt()).thenReturn(Instant.parse(instant));
        EventEligibilityResponse response = service.get(1L, 3L, 2L);
        var recent = response.checks().get(3);
        assertThat(response.baseDate()).hasToString("2026-11-17");
        assertThat(recent.passed()).isEqualTo(passed);
        assertThat(recent.detail().daysSince()).isEqualTo(days);
        assertThat(recent.detail().reason() == null ? null : recent.detail().reason().name()).isEqualTo(reason);
        assertThat(response.canApply()).isEqualTo(passed);
    }

    @Test
    void absentPolicyIsReportedAsNoDate() {
        when(policies.findFirstByStoreIdOrderByVersionDesc(2L)).thenReturn(Optional.empty());
        var response = service.get(1L, 3L, 2L);
        assertThat(response.canApply()).isFalse();
        assertThat(response.checks().get(3).detail().reason()).isEqualTo(EventEligibilityResponse.Reason.NO_DATE);
    }

    @Test
    void existingApplicationBlocksReapplicationWithoutChangingConditionResults() {
        EventApplication application = mock(EventApplication.class);
        when(application.getId()).thenReturn(99L);
        when(application.getStatus()).thenReturn(ApplicationStatus.IN_REVIEW);
        when(applications.findFirstByStoreIdAndEventIdAndStatusNotInOrderByIdDesc(eq(2L), eq(3L), any()))
                .thenReturn(Optional.of(application));
        var response = service.get(1L, 3L, 2L);
        assertThat(response.allPassed()).isTrue();
        assertThat(response.canApply()).isFalse();
        assertThat(response.existingApplication().applicationId()).isEqualTo("99");
    }

    @Test
    void missingRegionUnverifiedBusinessAndDisabledQrAreAllReported() {
        when(store.getRegionId()).thenReturn(null);
        when(store.getVerificationStatus()).thenReturn(StoreVerificationStatus.PENDING);
        when(qr.getStatus()).thenReturn(QrStatus.DISABLED);
        var response = service.get(1L, 3L, 2L);
        assertThat(response.checks().subList(0, 3)).allMatch(check -> !check.passed());
        assertThat(response.canApply()).isFalse();
    }

    @Test
    void activeQrWithoutTokenCannotBeUsedForParticipation() {
        when(qr.getQrTokenHash()).thenReturn(null);
        assertThat(service.get(1L, 3L, 2L).checks().get(2).passed()).isFalse();
    }

    @Test
    void closedRecruitmentBlocksEligibleStore() {
        when(recruitment.get(eq(3L), any())).thenReturn(new EventRecruitmentService.Recruitment(Set.of(11L), false));
        var response = service.get(1L, 3L, 2L);
        assertThat(response.allPassed()).isTrue();
        assertThat(response.recruitmentOpen()).isFalse();
        assertThat(response.canApply()).isFalse();
    }
}
