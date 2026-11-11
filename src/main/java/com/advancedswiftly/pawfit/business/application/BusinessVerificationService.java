package com.advancedswiftly.pawfit.business.application;

import com.advancedswiftly.pawfit.business.api.dto.StoreRequest;
import com.advancedswiftly.pawfit.business.api.dto.VerificationCreateRequest;
import com.advancedswiftly.pawfit.business.api.dto.VerificationCreateResponse;
import com.advancedswiftly.pawfit.business.api.dto.VerificationListItem;
import com.advancedswiftly.pawfit.business.api.dto.VerificationListResponse;
import com.advancedswiftly.pawfit.business.domain.Store;
import com.advancedswiftly.pawfit.business.domain.StoreVerification;
import com.advancedswiftly.pawfit.business.domain.VerificationStatus;
import com.advancedswiftly.pawfit.business.infrastructure.StoreRepository;
import com.advancedswiftly.pawfit.business.infrastructure.StoreVerificationRepository;
import com.advancedswiftly.pawfit.global.error.BusinessException;
import com.advancedswiftly.pawfit.global.error.ErrorCode;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BusinessVerificationService {

    private final StoreRepository storeRepository;
    private final StoreVerificationRepository verificationRepository;

    public BusinessVerificationService(
            StoreRepository storeRepository, StoreVerificationRepository verificationRepository) {
        this.storeRepository = storeRepository;
        this.verificationRepository = verificationRepository;
    }

    @Transactional
    public VerificationCreateResponse requestVerification(
            Long userId, VerificationCreateRequest request) {
        String businessNumber = normalizeBusinessNumber(request.businessNumber());

        // 반려(REJECTED)된 요청의 사업자등록번호만 다시 요청할 수 있다.
        if (verificationRepository.existsByBusinessNumberAndStatusNot(
                businessNumber, VerificationStatus.REJECTED)) {
            throw new BusinessException(ErrorCode.DUPLICATE_BUSINESS_NUMBER);
        }

        StoreRequest storeRequest = request.store();
        Store store = storeRepository.save(Store.registerPending(
                storeRequest.name(),
                storeRequest.phone(),
                storeRequest.address(),
                storeRequest.latitude(),
                storeRequest.longitude()));

        StoreVerification verification = StoreVerification.receive(
                store,
                userId,
                businessNumber,
                request.businessName(),
                request.representativeName(),
                request.documentUrl());

        try {
            // 동시 요청은 DB의 부분 unique index가 막으므로 여기서 flush해 위반을 바로 확인한다.
            verificationRepository.saveAndFlush(verification);
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ErrorCode.DUPLICATE_BUSINESS_NUMBER);
        }

        return VerificationCreateResponse.from(verification);
    }

    /** 본인이 제출한 인증 요청을 최신순으로 조회한다. status가 null이면 전체를 반환한다. */
    @Transactional(readOnly = true)
    public VerificationListResponse getMyVerifications(Long userId, VerificationStatus status) {
        List<StoreVerification> verifications = status == null
                ? verificationRepository.findBySubmittedByOrderByIdDesc(userId)
                : verificationRepository.findBySubmittedByAndStatusOrderByIdDesc(userId, status);

        return new VerificationListResponse(
                verifications.stream().map(VerificationListItem::from).toList());
    }

    private String normalizeBusinessNumber(String businessNumber) {
        return businessNumber.replace("-", "");
    }
}
