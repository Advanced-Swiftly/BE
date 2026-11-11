package com.advancedswiftly.pawfit.business.application;

import com.advancedswiftly.pawfit.business.api.dto.ConditionRequestActionRequest;
import com.advancedswiftly.pawfit.business.api.dto.ConditionRequestCreateRequest;
import com.advancedswiftly.pawfit.business.api.dto.ConditionRequestListItem;
import com.advancedswiftly.pawfit.business.api.dto.ConditionRequestListResponse;
import com.advancedswiftly.pawfit.business.api.dto.ConditionRequestDetailResponse;
import com.advancedswiftly.pawfit.business.api.dto.ConditionRequestResultResponse;
import com.advancedswiftly.pawfit.business.domain.ConditionRequest;
import com.advancedswiftly.pawfit.business.domain.ConditionRequestStatus;
import com.advancedswiftly.pawfit.business.infrastructure.ConditionRequestRepository;
import com.advancedswiftly.pawfit.business.infrastructure.StorePolicyRepository;
import com.advancedswiftly.pawfit.global.error.BusinessException;
import com.advancedswiftly.pawfit.global.error.ErrorCode;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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

    public static final int PAGE_SIZE = 20;

    /** 업체의 요청 목록. 최신순이며 page는 1부터 시작한다. */
    @Transactional(readOnly = true)
    public ConditionRequestListResponse list(
            Long userId, Long storeId, ConditionRequestStatus status, int page) {
        managedStoreChecker.requireManagedStore(userId, storeId);
        PageRequest pageable = PageRequest.of(page - 1, PAGE_SIZE);
        Page<ConditionRequest> result = status == null
                ? requestRepository.findByStoreIdOrderByIdDesc(storeId, pageable)
                : requestRepository.findByStoreIdAndStatusOrderByIdDesc(storeId, status, pageable);
        return new ConditionRequestListResponse(
                result.getContent().stream().map(ConditionRequestListItem::from).toList());
    }

    /** 본인 요청을 취소하거나 보완해 다시 제출한다. 없거나 본인 요청이 아니면 404, 상태가 맞지 않으면 400. */
    @Transactional
    public ConditionRequestResultResponse act(Long userId, Long requestId, ConditionRequestActionRequest request) {
        ConditionRequest conditionRequest = requestRepository
                .findByIdAndSubmittedBy(requestId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        switch (request.action()) {
            case CANCEL -> {
                if (!conditionRequest.canCancel()) {
                    throw new BusinessException(ErrorCode.INVALID_REQUEST_STATE, "취소할 수 없는 상태입니다.");
                }
                conditionRequest.cancel();
            }
            case RESUBMIT -> {
                if (!conditionRequest.canResubmit()) {
                    throw new BusinessException(ErrorCode.INVALID_REQUEST_STATE, "보완 요청된 건만 다시 제출할 수 있습니다.");
                }
                conditionRequest.resubmit(
                        request.requestedPolicy() == null ? null : request.requestedPolicy().toRules(),
                        request.sourceText(),
                        request.evidenceUrls());
            }
        }
        return ConditionRequestResultResponse.from(conditionRequest);
    }
}
