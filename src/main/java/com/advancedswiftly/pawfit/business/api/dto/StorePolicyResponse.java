package com.advancedswiftly.pawfit.business.api.dto;

import com.advancedswiftly.pawfit.business.domain.PolicyRule;
import com.advancedswiftly.pawfit.business.domain.PolicyRuleType;
import com.advancedswiftly.pawfit.business.domain.StorePolicy;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "업체에 적용 중인 입장 정책")
public record StorePolicyResponse(
        @Schema(description = "정책 ID", example = "1201") Long policyId,
        @Schema(description = "정책 버전", example = "3") int version,
        @Schema(description = "견종 조건") List<PolicyRuleDto> breedRules,
        @Schema(description = "장비 조건") List<PolicyRuleDto> equipmentRules,
        @Schema(description = "접종 조건") List<PolicyRuleDto> vaccineRules) {

    public static StorePolicyResponse from(StorePolicy policy) {
        return new StorePolicyResponse(
                policy.getId(),
                policy.getVersion(),
                rulesOf(policy, PolicyRuleType.BREED),
                rulesOf(policy, PolicyRuleType.EQUIPMENT),
                rulesOf(policy, PolicyRuleType.VACCINE));
    }

    private static List<PolicyRuleDto> rulesOf(StorePolicy policy, PolicyRuleType type) {
        return policy.getRules().stream()
                .filter(rule -> rule.getType() == type)
                .map(PolicyRuleDto::from)
                .toList();
    }
}
