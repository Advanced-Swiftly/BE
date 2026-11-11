package com.advancedswiftly.pawfit.business.application;

import com.advancedswiftly.pawfit.business.api.dto.StorePolicyResponse;
import com.advancedswiftly.pawfit.business.api.dto.StoreProfileUpdateRequest;
import com.advancedswiftly.pawfit.business.api.dto.StoreProfileUpdateResponse;
import com.advancedswiftly.pawfit.business.api.dto.StoreQrResponse;
import com.advancedswiftly.pawfit.business.domain.Store;
import com.advancedswiftly.pawfit.business.domain.StoreQr;
import com.advancedswiftly.pawfit.business.infrastructure.StorePolicyRepository;
import com.advancedswiftly.pawfit.business.infrastructure.StoreQrRepository;
import com.advancedswiftly.pawfit.business.infrastructure.StoreRepository;
import com.advancedswiftly.pawfit.global.error.BusinessException;
import com.advancedswiftly.pawfit.global.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 관리 업체의 기본 정보, 현재 입장 정책, QR을 다룬다. */
@Service
public class StoreOperationService {

    private final StoreRepository storeRepository;
    private final StorePolicyRepository policyRepository;
    private final StoreQrRepository qrRepository;
    private final ManagedStoreChecker managedStoreChecker;

    public StoreOperationService(
            StoreRepository storeRepository,
            StorePolicyRepository policyRepository,
            StoreQrRepository qrRepository,
            ManagedStoreChecker managedStoreChecker) {
        this.storeRepository = storeRepository;
        this.policyRepository = policyRepository;
        this.qrRepository = qrRepository;
        this.managedStoreChecker = managedStoreChecker;
    }

    @Transactional
    public StoreProfileUpdateResponse updateProfile(
            Long userId, Long storeId, StoreProfileUpdateRequest request) {
        managedStoreChecker.requireManagedStore(userId, storeId);
        Store store = storeRepository.findById(storeId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        store.updateProfile(request.name(), request.phoneNumber(), request.description());
        if (request.amenities() != null) {
            store.replaceAmenities(request.amenities());
        }
        return StoreProfileUpdateResponse.from(store);
    }

    /** 현재 정책(가장 큰 version)을 조회한다. 아직 정책이 없는 업체는 404. */
    @Transactional(readOnly = true)
    public StorePolicyResponse getCurrentPolicy(Long userId, Long storeId) {
        managedStoreChecker.requireManagedStore(userId, storeId);
        return policyRepository
                .findFirstByStoreIdOrderByVersionDesc(storeId)
                .map(StorePolicyResponse::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public StoreQrResponse getQr(Long userId, Long storeId) {
        managedStoreChecker.requireManagedStore(userId, storeId);
        return StoreQrResponse.withImage(currentQr(storeId));
    }

    @Transactional
    public StoreQrResponse changeQrActive(Long userId, Long storeId, boolean active) {
        managedStoreChecker.requireManagedStore(userId, storeId);
        StoreQr qr = currentQr(storeId);
        qr.changeActive(active);
        return StoreQrResponse.statusOnly(qr);
    }

    private StoreQr currentQr(Long storeId) {
        return qrRepository
                .findFirstByStoreIdOrderByIdDesc(storeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }
}
