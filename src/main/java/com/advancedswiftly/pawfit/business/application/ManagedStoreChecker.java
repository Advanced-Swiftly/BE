package com.advancedswiftly.pawfit.business.application;

import com.advancedswiftly.pawfit.business.domain.StoreManager;
import com.advancedswiftly.pawfit.business.domain.VerificationStatus;
import com.advancedswiftly.pawfit.business.infrastructure.StoreManagerRepository;
import com.advancedswiftly.pawfit.business.infrastructure.StoreVerificationRepository;
import com.advancedswiftly.pawfit.global.error.BusinessException;
import com.advancedswiftly.pawfit.global.error.ErrorCode;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * 업체 관리자가 신청할 수 있는 "관리 업체"인지 확인한다.
 * 업체에 관리자(store_manager)가 지정되어 있으면 그 사용자만, 지정되지 않은 업체는
 * 본인이 인증을 요청해 승인된 경우에만 관리 업체로 본다.
 */
@Component
public class ManagedStoreChecker {

    private final StoreVerificationRepository verificationRepository;
    private final StoreManagerRepository managerRepository;

    public ManagedStoreChecker(
            StoreVerificationRepository verificationRepository, StoreManagerRepository managerRepository) {
        this.verificationRepository = verificationRepository;
        this.managerRepository = managerRepository;
    }

    public void requireManagedStore(Long userId, Long storeId) {
        Optional<StoreManager> manager = managerRepository.findById(storeId);
        boolean allowed = manager.isPresent()
                ? manager.get().getUserId().equals(userId)
                : verificationRepository.existsByStoreIdAndSubmittedByAndStatus(
                        storeId, userId, VerificationStatus.APPROVED);
        if (!allowed) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}
