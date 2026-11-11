package com.advancedswiftly.pawfit.business.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/** 입장 정책의 조건 하나(견종·장비·접종). 정책과 변경 요청이 같이 사용한다. */
@Embeddable
public class PolicyRule {

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false, length = 20)
    private PolicyRuleType type;

    @Column(name = "rule_code", nullable = false, length = 80)
    private String code;

    @Column(name = "rule_required", nullable = false)
    private boolean required;

    protected PolicyRule() {
    }

    public PolicyRule(PolicyRuleType type, String code, boolean required) {
        this.type = type;
        this.code = code;
        this.required = required;
    }

    public PolicyRuleType getType() {
        return type;
    }

    public String getCode() {
        return code;
    }

    public boolean isRequired() {
        return required;
    }
}
