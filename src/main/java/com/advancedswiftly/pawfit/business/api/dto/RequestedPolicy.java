package com.advancedswiftly.pawfit.business.api.dto;

import com.advancedswiftly.pawfit.business.domain.PolicyRule;
import com.advancedswiftly.pawfit.business.domain.PolicyRuleType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

/** 변경 요청의 구조화 정책. 현재 정책 조회 응답과 같은 형태이며, 생략한 조건은 비어 있는 것으로 본다. */
@Schema(description = "변경할 구조화 정책")
public record RequestedPolicy(
        @Schema(description = "견종 조건") @Valid @Size(max = 100, message = "견종 조건은 100개 이하여야 합니다.")
        List<PolicyRuleDto> breedRules,
        @Schema(description = "장비 조건") @Valid @Size(max = 100, message = "장비 조건은 100개 이하여야 합니다.")
        List<PolicyRuleDto> equipmentRules,
        @Schema(description = "접종 조건") @Valid @Size(max = 100, message = "접종 조건은 100개 이하여야 합니다.")
        List<PolicyRuleDto> vaccineRules) {

    /** 종류별 조건을 하나의 목록으로 합친다. 같은 종류 안에서 코드가 겹치면 먼저 나온 것만 쓴다. */
    public List<PolicyRule> toRules() {
        List<PolicyRule> rules = new ArrayList<>();
        addAll(rules, PolicyRuleType.BREED, breedRules);
        addAll(rules, PolicyRuleType.EQUIPMENT, equipmentRules);
        addAll(rules, PolicyRuleType.VACCINE, vaccineRules);
        return rules;
    }

    private static void addAll(List<PolicyRule> target, PolicyRuleType type, List<PolicyRuleDto> source) {
        if (source == null) {
            return;
        }
        for (PolicyRuleDto dto : source) {
            boolean duplicated = target.stream()
                    .anyMatch(rule -> rule.getType() == type && rule.getCode().equals(dto.code()));
            if (!duplicated) {
                target.add(new PolicyRule(type, dto.code(), dto.required()));
            }
        }
    }
}
