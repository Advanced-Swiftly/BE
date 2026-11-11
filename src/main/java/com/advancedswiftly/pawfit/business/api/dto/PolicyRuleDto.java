package com.advancedswiftly.pawfit.business.api.dto;

import com.advancedswiftly.pawfit.business.domain.PolicyRule;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "입장 정책의 조건 하나")
public record PolicyRuleDto(
        @Schema(description = "조건 코드", example = "CARRIER")
        @NotBlank(message = "조건 코드는 필수입니다.")
        @Size(max = 80, message = "조건 코드는 80자 이하여야 합니다.")
        String code,

        @Schema(description = "필수 여부", example = "true")
        @NotNull(message = "필수 여부는 필수입니다.")
        Boolean required) {

    public static PolicyRuleDto from(PolicyRule rule) {
        return new PolicyRuleDto(rule.getCode(), rule.isRequired());
    }
}
