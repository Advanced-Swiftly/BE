package com.advancedswiftly.pawfit.business.application;

import com.advancedswiftly.pawfit.business.api.dto.ConditionRequestCreateRequest;
import com.advancedswiftly.pawfit.business.api.dto.ConditionRequestDetailResponse;
import com.advancedswiftly.pawfit.business.api.dto.ConditionRequestResultResponse;
import com.advancedswiftly.pawfit.business.domain.ConditionRequest;
import com.advancedswiftly.pawfit.business.infrastructure.ConditionRequestRepository;
import com.advancedswiftly.pawfit.business.infrastructure.StorePolicyRepository;
import com.advancedswiftly.pawfit.global.error.BusinessException;
import com.advancedswiftly.pawfit.global.error.ErrorCode;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConditionRequestService {

    private final ConditionRequestRepository requestRepository;
    private final StorePolicyRepository policyRepository;
    private final ManagedStoreChecker managedStoreChecker;

    public ConditionRequestService(
            ConditionRequestRepository requestRepository,
            StorePolicyRepository policyRepository,
            ManagedStoreChecker managedStoreChecker) {
        this.requestRepository = requestRepository;
        this.policyRepository = policyRepository;
        this.managedStoreChecker = managedStoreChecker;
    }

    /**
     * 현재 정책 버전을 기준으로 변경안을 제출한다.
     * 기준 버전이 현재 버전과 다르면(그 사이 정책이 바뀐 경우) POLICY_VERSION_CONFLICT(409)로 응답한다.
     */
    @Transactional
    public ConditionRequestResultResponse create(
            Long userId, Long storeId, ConditionRequestCreateRequest request) {
        managedStoreChecker.requireManagedStore(userId, storeId);

        int currentVersion = policyRepository
                .findFirstByStoreIdOrderByVersionDesc(storeId)
                .map(policy -> policy.getVersion())
                .orElse(0);
        if (request.baseVersion() != currentVersion) {
            throw new BusinessException(ErrorCode.POLICY_VERSION_CONFLICT);
        }

        List<String> evidenceUrls = request.evidenceUrls() == null ? List.of() : request.evidenceUrls();
        ConditionRequest conditionRequest = requestRepository.save(ConditionRequest.receive(
                storeId,
                request.baseVersion(),
                request.sourceText(),
                userId,
                request.requestedPolicy().toRules(),
                evidenceUrls));
        return ConditionRequestResultResponse.from(conditionRequest);
    }

    /** 본인이 제출한 요청만 조회한다. 없거나 본인 요청이 아니면 404. */
    @Transactional(readOnly = true)
    public ConditionRequestDetailResponse get(Long userId, Long requestId) {
        return requestRepository
                .findByIdAndSubmittedBy(requestId, userId)
                .map(ConditionRequestDetailResponse::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }
}
