package com.advancedswiftly.pawfit.business.application;

import com.advancedswiftly.pawfit.business.api.dto.ApplicationResultResponse;
import com.advancedswiftly.pawfit.business.api.dto.EventApplicationCreateRequest;
import com.advancedswiftly.pawfit.business.api.dto.EventApplicationDetailResponse;
import com.advancedswiftly.pawfit.business.api.dto.EventApplicationListItem;
import com.advancedswiftly.pawfit.business.api.dto.EventApplicationListResponse;
import com.advancedswiftly.pawfit.business.domain.ApplicationStatus;
import com.advancedswiftly.pawfit.business.domain.EventApplication;
import com.advancedswiftly.pawfit.business.infrastructure.EventApplicationRepository;
import com.advancedswiftly.pawfit.event.domain.EventStatus;
import com.advancedswiftly.pawfit.event.domain.StampEvent;
import com.advancedswiftly.pawfit.event.infrastructure.StampEventRepository;
import com.advancedswiftly.pawfit.global.error.BusinessException;
import com.advancedswiftly.pawfit.global.error.ErrorCode;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventApplicationService {

    /** 이 상태의 신청은 같은 업체가 같은 이벤트에 다시 신청할 수 있다. */
    private static final List<ApplicationStatus> REAPPLY_ALLOWED =
            List.of(ApplicationStatus.REJECTED, ApplicationStatus.CANCELED);

    private final EventApplicationRepository applicationRepository;
    private final StampEventRepository eventRepository;
    private final ManagedStoreChecker managedStoreChecker;

    public EventApplicationService(
            EventApplicationRepository applicationRepository,
            StampEventRepository eventRepository,
            ManagedStoreChecker managedStoreChecker) {
        this.applicationRepository = applicationRepository;
        this.eventRepository = eventRepository;
        this.managedStoreChecker = managedStoreChecker;
    }

    @Transactional
    public ApplicationResultResponse apply(Long userId, EventApplicationCreateRequest request) {
        managedStoreChecker.requireManagedStore(userId, request.storeId());

        StampEvent event = eventRepository
                .findDetailById(request.eventId())
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_NOT_FOUND));
        if (event.getStatus() != EventStatus.OPEN) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "신청할 수 없는 이벤트입니다.");
        }

        Set<Long> regionIds = new LinkedHashSet<>(request.regionIds());
        if (!event.getRegionIds().containsAll(regionIds)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "이벤트 참여 지역에 없는 지역이 포함되어 있습니다.");
        }

        if (applicationRepository.existsByStoreIdAndEventIdAndStatusNotIn(
                request.storeId(), request.eventId(), REAPPLY_ALLOWED)) {
            throw new BusinessException(ErrorCode.DUPLICATE_APPLICATION);
        }

        EventApplication application = EventApplication.receive(
                request.storeId(), request.eventId(), userId, regionIds, request.message());
        try {
            // 동시 신청은 DB의 부분 unique index가 막으므로 여기서 flush해 위반을 바로 확인한다.
            applicationRepository.saveAndFlush(application);
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ErrorCode.DUPLICATE_APPLICATION);
        }
        return ApplicationResultResponse.of(application.getId(), application.getStatus());
    }

    /** 본인이 낸 신청을 최신순으로 조회한다. storeId, status가 null이면 해당 조건은 적용하지 않는다. */
    @Transactional(readOnly = true)
    public EventApplicationListResponse list(Long userId, Long storeId, ApplicationStatus status) {
        List<EventApplicationListItem> items = applicationRepository.findBySubmittedByOrderByIdDesc(userId).stream()
                .filter(application -> storeId == null || storeId.equals(application.getStoreId()))
                .filter(application -> status == null || application.getStatus() == status)
                .map(EventApplicationListItem::from)
                .toList();
        return new EventApplicationListResponse(items);
    }

    @Transactional(readOnly = true)
    public EventApplicationDetailResponse get(Long userId, Long applicationId) {
        return EventApplicationDetailResponse.from(find(userId, applicationId));
    }

    @Transactional
    public ApplicationResultResponse cancel(Long userId, Long applicationId) {
        EventApplication application = find(userId, applicationId);
        if (!application.canCancel()) {
            throw new BusinessException(ErrorCode.CANCEL_NOT_ALLOWED);
        }
        application.cancel();
        return ApplicationResultResponse.of(application.getId(), application.getStatus());
    }

    private EventApplication find(Long userId, Long applicationId) {
        return applicationRepository
                .findByIdAndSubmittedBy(applicationId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }
}
