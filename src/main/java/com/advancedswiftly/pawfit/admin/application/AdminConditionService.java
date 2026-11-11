package com.advancedswiftly.pawfit.admin.application;

import com.advancedswiftly.pawfit.admin.api.dto.AdminConditionRequestDetailResponse;
import com.advancedswiftly.pawfit.admin.api.dto.AdminPolicyView;
import com.advancedswiftly.pawfit.admin.api.dto.ConditionReviewRequest;
import com.advancedswiftly.pawfit.admin.api.dto.ConditionReviewResponse;
import com.advancedswiftly.pawfit.admin.api.dto.PolicyChangeItem;
import com.advancedswiftly.pawfit.admin.api.dto.PolicyChangeRequest;
import com.advancedswiftly.pawfit.admin.api.dto.PolicyChangeResponse;
import com.advancedswiftly.pawfit.business.api.dto.StorePolicyResponse;
import com.advancedswiftly.pawfit.business.domain.ConditionRequest;
import com.advancedswiftly.pawfit.business.domain.ConditionRequestKind;
import com.advancedswiftly.pawfit.business.domain.ConditionRequestStatus;
import com.advancedswiftly.pawfit.business.domain.PolicyRule;
import com.advancedswiftly.pawfit.business.domain.ReviewAction;
import com.advancedswiftly.pawfit.business.domain.StorePolicy;
import com.advancedswiftly.pawfit.business.infrastructure.ConditionRequestRepository;
import com.advancedswiftly.pawfit.business.infrastructure.StorePolicyRepository;
import com.advancedswiftly.pawfit.business.infrastructure.StoreRepository;
import com.advancedswiftly.pawfit.global.error.BusinessException;
import com.advancedswiftly.pawfit.global.error.ErrorCode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** 운영자의 입장 조건 요청 심사와 정책 직접 변경. */
@Service
public class AdminConditionService {

    private static final String CHANGED_BY_OPERATOR = "OPERATOR";

    private final ConditionRequestRepository requestRepository;
    private final StorePolicyRepository policyRepository;
    private final StoreRepository storeRepository;

    public AdminConditionService(
            ConditionRequestRepository requestRepository,
            StorePolicyRepository policyRepository,
            StoreRepository storeRepository) {
        this.requestRepository = requestRepository;
        this.policyRepository = policyRepository;
        this.storeRepository = storeRepository;
    }

    @Transactional(readOnly = true)
    public AdminConditionRequestDetailResponse get(Long requestId) {
        ConditionRequest request = find(requestId);
        Optional<StorePolicy> current = policyRepository.findFirstByStoreIdOrderByVersionDesc(request.getStoreId());
        List<PolicyRule> requested = request.getRequestedRules();
        return new AdminConditionRequestDetailResponse(
                request.getId(),
                request.getStoreId(),
                request.getKind().name(),
                request.getStatus(),
                request.getBaseVersion(),
                request.getSourceText(),
                current.map(StorePolicyResponse::from).orElse(null),
                AdminPolicyView.from(requested),
                diff(current.map(StorePolicy::getRules).orElse(List.of()), requested),
                List.copyOf(request.getEvidenceUrls()),
                request.getReviewNote(),
                request.getReviewedAt(),
                request.getCreatedAt());
    }

    @Transactional
    public ConditionReviewResponse review(Long operatorId, Long requestId, ConditionReviewRequest body) {
        ConditionRequest request = find(requestId);
        ReviewAction action = ReviewAction.valueOf(body.action().name());
        ConditionRequestStatus target = request.getStatus().afterReview(action);
        if (target == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_STATE, "현재 상태에서 처리할 수 없는 동작입니다.");
        }
        boolean needsComment = action == ReviewAction.NEEDS_INFO || action == ReviewAction.REJECT;
        if (needsComment && !StringUtils.hasText(body.reviewComment())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "보완·반려 사유는 필수입니다.");
        }
        Integer appliedVersion = null;
        if (action == ReviewAction.APPROVE) {
            if (request.getKind() == ConditionRequestKind.RECHECK && body.approvedPolicy() == null) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "재검토 요청은 적용할 정책을 함께 보내야 승인할 수 있습니다.");
            }
            int currentVersion = currentVersion(request.getStoreId());
            if (request.getBaseVersion() != currentVersion) {
                throw new BusinessException(ErrorCode.POLICY_VERSION_CONFLICT);
            }
            List<PolicyRule> rules = body.approvedPolicy() != null
                    ? body.approvedPolicy().toRules()
                    : new ArrayList<>(request.getRequestedRules());
            appliedVersion = currentVersion + 1;
            policyRepository.save(StorePolicy.createFromRequest(
                    request.getStoreId(),
                    appliedVersion,
                    request.getSourceText(),
                    rules,
                    CHANGED_BY_OPERATOR,
                    request.getId()));
        }
        request.review(target, body.reviewComment(), operatorId);
        return new ConditionReviewResponse(request.getId(), request.getStatus(), appliedVersion);
    }

    @Transactional
    public PolicyChangeResponse changePolicy(Long operatorId, Long storeId, PolicyChangeRequest body) {
        if (!storeRepository.existsById(storeId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        int currentVersion = currentVersion(storeId);
        if (body.baseVersion() != currentVersion) {
            throw new BusinessException(ErrorCode.POLICY_VERSION_CONFLICT);
        }
        List<PolicyRule> rules = body.policy().toRules();
        ConditionRequest record = requestRepository.save(ConditionRequest.operatorDirect(
                storeId, currentVersion, body.sourceText(), operatorId, rules, body.reason()));
        int appliedVersion = currentVersion + 1;
        policyRepository.save(StorePolicy.createFromRequest(
                storeId, appliedVersion, body.sourceText(), rules, CHANGED_BY_OPERATOR, record.getId()));
        return new PolicyChangeResponse(record.getId(), record.getKind().name(), record.getStatus(), appliedVersion);
    }

    private ConditionRequest find(Long requestId) {
        return requestRepository.findById(requestId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    private int currentVersion(Long storeId) {
        return policyRepository
                .findFirstByStoreIdOrderByVersionDesc(storeId)
                .map(StorePolicy::getVersion)
                .orElse(0);
    }

    private static List<PolicyChangeItem> diff(List<PolicyRule> current, List<PolicyRule> requested) {
        List<PolicyChangeItem> changes = new ArrayList<>();
        for (PolicyRule after : requested) {
            Optional<PolicyRule> before = current.stream()
                    .filter(rule -> rule.getType() == after.getType() && rule.getCode().equals(after.getCode()))
                    .findFirst();
            if (before.isEmpty()) {
                changes.add(new PolicyChangeItem(after.getType(), after.getCode(), "ADDED", null, after.isRequired()));
            } else if (before.get().isRequired() != after.isRequired()) {
                changes.add(new PolicyChangeItem(
                        after.getType(), after.getCode(), "REQUIRED_CHANGED", before.get().isRequired(), after.isRequired()));
            }
        }
        for (PolicyRule before : current) {
            boolean kept = requested.stream()
                    .anyMatch(rule -> rule.getType() == before.getType() && rule.getCode().equals(before.getCode()));
            if (!kept) {
                changes.add(new PolicyChangeItem(before.getType(), before.getCode(), "REMOVED", before.isRequired(), null));
            }
        }
        return changes;
    }
}
