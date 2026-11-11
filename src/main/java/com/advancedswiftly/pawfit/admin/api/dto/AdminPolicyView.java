package com.advancedswiftly.pawfit.admin.api.dto;

import com.advancedswiftly.pawfit.business.api.dto.PolicyRuleDto;
import com.advancedswiftly.pawfit.business.domain.PolicyRule;
import com.advancedswiftly.pawfit.business.domain.PolicyRuleType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "구조화된 입장 정책")
public record AdminPolicyView(
        @Schema(description = "견종 조건") List<PolicyRuleDto> breedRules,
        @Schema(description = "장비 조건") List<PolicyRuleDto> equipmentRules,
        @Schema(description = "접종 조건") List<PolicyRuleDto> vaccineRules) {

    public static AdminPolicyView from(List<PolicyRule> rules) {
        return new AdminPolicyView(
                of(rules, PolicyRuleType.BREED), of(rules, PolicyRuleType.EQUIPMENT), of(rules, PolicyRuleType.VACCINE));
    }

    private static List<PolicyRuleDto> of(List<PolicyRule> rules, PolicyRuleType type) {
        return rules.stream().filter(rule -> rule.getType() == type).map(PolicyRuleDto::from).toList();
    }
}
