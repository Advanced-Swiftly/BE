package com.advancedswiftly.pawfit.business.application;

import com.advancedswiftly.pawfit.business.domain.VerificationStatus;
import com.advancedswiftly.pawfit.business.infrastructure.StoreVerificationRepository;
import com.advancedswiftly.pawfit.global.error.BusinessException;
import com.advancedswiftly.pawfit.global.error.ErrorCode;
import org.springframework.stereotype.Component;

/**
 * 업체 관리자가 신청할 수 있는 "관리 업체"인지 확인한다.
 * 업체-관리자 연결 테이블(store_manager)이 생기기 전까지는 본인이 인증을 요청해 승인된 업체만 관리 업체로 본다.
 */
@Component
public class ManagedStoreChecker {

    private final StoreVerificationRepository verificationRepository;

    public ManagedStoreChecker(StoreVerificationRepository verificationRepository) {
        this.verificationRepository = verificationRepository;
    }

    public void requireManagedStore(Long userId, Long storeId) {
        if (!verificationRepository.existsByStoreIdAndSubmittedByAndStatus(
                storeId, userId, VerificationStatus.APPROVED)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}
