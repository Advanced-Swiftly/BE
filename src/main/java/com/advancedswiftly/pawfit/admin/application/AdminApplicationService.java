package com.advancedswiftly.pawfit.admin.application;

import com.advancedswiftly.pawfit.admin.api.dto.AdminAdApplicationDetailResponse;
import com.advancedswiftly.pawfit.admin.api.dto.ApplicationReviewRequest;
import com.advancedswiftly.pawfit.admin.api.dto.ApplicationReviewResponse;
import com.advancedswiftly.pawfit.business.application.EventStoreSynchronizer;
import com.advancedswiftly.pawfit.business.domain.AdApplication;
import com.advancedswiftly.pawfit.business.domain.ApplicationStatus;
import com.advancedswiftly.pawfit.business.domain.EventApplication;
import com.advancedswiftly.pawfit.business.domain.QrStatus;
import com.advancedswiftly.pawfit.business.domain.ReviewAction;
import com.advancedswiftly.pawfit.business.domain.Store;
import com.advancedswiftly.pawfit.business.domain.StoreQr;
import com.advancedswiftly.pawfit.business.infrastructure.AdApplicationRepository;
import com.advancedswiftly.pawfit.business.infrastructure.EventApplicationRepository;
import com.advancedswiftly.pawfit.business.infrastructure.StoreQrRepository;
import com.advancedswiftly.pawfit.business.infrastructure.StoreRepository;
import com.advancedswiftly.pawfit.global.error.BusinessException;
import com.advancedswiftly.pawfit.global.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** 운영자의 광고·이벤트 참여 신청 심사. */
@Service
public class AdminApplicationService {

    private final AdApplicationRepository adRepository;
    private final EventApplicationRepository eventRepository;
    private final StoreRepository storeRepository;
    private final StoreQrRepository qrRepository;
    private final EventStoreSynchronizer eventStoreSynchronizer;

    public AdminApplicationService(
            AdApplicationRepository adRepository,
            EventApplicationRepository eventRepository,
            StoreRepository storeRepository,
            StoreQrRepository qrRepository,
            EventStoreSynchronizer eventStoreSynchronizer) {
        this.adRepository = adRepository;
        this.eventRepository = eventRepository;
        this.storeRepository = storeRepository;
        this.qrRepository = qrRepository;
        this.eventStoreSynchronizer = eventStoreSynchronizer;
    }

    @Transactional(readOnly = true)
    public AdminAdApplicationDetailResponse getAd(Long applicationId) {
        AdApplication application =
                adRepository.findById(applicationId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        String storeName = storeRepository.findById(application.getStoreId()).map(Store::getName).orElse(null);
        return AdminAdApplicationDetailResponse.of(application, storeName);
    }

    @Transactional
    public ApplicationReviewResponse reviewAd(Long reviewerId, Long applicationId, ApplicationReviewRequest request) {
        AdApplication application =
                adRepository.findById(applicationId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        ReviewAction action = toDomain(request);
        ApplicationStatus target = target(application.getStatus(), action, request.comment());
        application.review(target, comment(request), reviewerId);
        return new ApplicationReviewResponse(application.getId(), application.getStatus());
    }

    /** 승인되면 업체의 QR이 있는 경우 event_store에도 같은 ID로 참여 업체로 올린다. */
    @Transactional
    public ApplicationReviewResponse reviewEvent(Long reviewerId, Long applicationId, ApplicationReviewRequest request) {
        EventApplication application =
                eventRepository.findById(applicationId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        ReviewAction action = toDomain(request);
        ApplicationStatus target = target(application.getStatus(), action, request.comment());
        application.review(target, comment(request), reviewerId);
        if (target == ApplicationStatus.APPROVED) {
            syncEventStore(application);
        }
        return new ApplicationReviewResponse(application.getId(), application.getStatus());
    }

    private void syncEventStore(EventApplication application) {
        Store store = storeRepository.findById(application.getStoreId()).orElse(null);
        if (store == null) {
            return;
        }
        StoreQr qr = qrRepository.findFirstByStoreIdOrderByIdDesc(store.getId()).orElse(null);
        if (qr == null || qr.getQrTokenHash() == null || qr.getStatus() == QrStatus.REVOKED) {
            return; // QR을 발급하는 시점에 event_store가 맞춰진다.
        }
        Long regionId = store.getRegionId() != null
                ? store.getRegionId()
                : application.getRegionIds().stream().findFirst().orElse(null);
        eventStoreSynchronizer.syncApproved(
                store, application.getEventId(), regionId, qr.getQrTokenHash(), qr.getStatus() != QrStatus.ACTIVE);
    }

    private static ReviewAction toDomain(ApplicationReviewRequest request) {
        return ReviewAction.valueOf(request.action().name());
    }

    /** 현재 상태에서 할 수 없는 동작이면 INVALID_REQUEST_STATE, 사유가 필요한데 없으면 400. */
    private static ApplicationStatus target(ApplicationStatus current, ReviewAction action, String comment) {
        if ((action == ReviewAction.NEEDS_INFO || action == ReviewAction.REJECT) && !StringUtils.hasText(comment)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "보완·반려 사유는 필수입니다.");
        }
        ApplicationStatus target = current.afterReview(action);
        if (target == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_STATE, "현재 상태에서는 처리할 수 없습니다.");
        }
        return target;
    }

    private static String comment(ApplicationReviewRequest request) {
        return StringUtils.hasText(request.comment()) ? request.comment().trim() : null;
    }
}
