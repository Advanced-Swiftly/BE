package com.advancedswiftly.pawfit.business.application;

import com.advancedswiftly.pawfit.business.api.dto.AdApplicationCreateRequest;
import com.advancedswiftly.pawfit.business.api.dto.AdApplicationDetailResponse;
import com.advancedswiftly.pawfit.business.api.dto.AdApplicationListItem;
import com.advancedswiftly.pawfit.business.api.dto.AdApplicationListResponse;
import com.advancedswiftly.pawfit.business.api.dto.ApplicationResultResponse;
import com.advancedswiftly.pawfit.business.domain.AdApplication;
import com.advancedswiftly.pawfit.business.domain.ApplicationStatus;
import com.advancedswiftly.pawfit.business.infrastructure.AdApplicationRepository;
import com.advancedswiftly.pawfit.global.error.BusinessException;
import com.advancedswiftly.pawfit.global.error.ErrorCode;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdApplicationService {

    private final AdApplicationRepository applicationRepository;
    private final ManagedStoreChecker managedStoreChecker;

    public AdApplicationService(
            AdApplicationRepository applicationRepository, ManagedStoreChecker managedStoreChecker) {
        this.applicationRepository = applicationRepository;
        this.managedStoreChecker = managedStoreChecker;
    }

    @Transactional
    public ApplicationResultResponse apply(Long userId, AdApplicationCreateRequest request) {
        managedStoreChecker.requireManagedStore(userId, request.storeId());

        if (request.endDate().isBefore(request.startDate())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "종료일은 시작일보다 빠를 수 없습니다.");
        }

        AdApplication application = applicationRepository.save(AdApplication.receive(
                request.storeId(),
                userId,
                request.adType(),
                request.startDate(),
                request.endDate(),
                request.creativeUrl(),
                request.message()));
        return ApplicationResultResponse.of(application.getId(), application.getStatus());
    }

    /** 본인이 낸 신청을 최신순으로 조회한다. storeId, status가 null이면 해당 조건은 적용하지 않는다. */
    @Transactional(readOnly = true)
    public AdApplicationListResponse list(Long userId, Long storeId, ApplicationStatus status) {
        List<AdApplicationListItem> items = applicationRepository.findBySubmittedByOrderByIdDesc(userId).stream()
                .filter(application -> storeId == null || storeId.equals(application.getStoreId()))
                .filter(application -> status == null || application.getStatus() == status)
                .map(AdApplicationListItem::from)
                .toList();
        return new AdApplicationListResponse(items);
    }

    @Transactional(readOnly = true)
    public AdApplicationDetailResponse get(Long userId, Long applicationId) {
        return AdApplicationDetailResponse.from(find(userId, applicationId));
    }

    @Transactional
    public ApplicationResultResponse cancel(Long userId, Long applicationId) {
        AdApplication application = find(userId, applicationId);
        if (!application.canCancel()) {
            throw new BusinessException(ErrorCode.CANCEL_NOT_ALLOWED);
        }
        application.cancel();
        return ApplicationResultResponse.of(application.getId(), application.getStatus());
    }

    private AdApplication find(Long userId, Long applicationId) {
        return applicationRepository
                .findByIdAndSubmittedBy(applicationId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }
}
