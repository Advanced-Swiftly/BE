package com.advancedswiftly.pawfit.auth.api;

import com.advancedswiftly.pawfit.auth.api.dto.CommonDtos.BreedsResponse;
import com.advancedswiftly.pawfit.auth.api.dto.CommonDtos.RegionsResponse;
import com.advancedswiftly.pawfit.auth.api.dto.CommonDtos.TermsResponse;
import com.advancedswiftly.pawfit.auth.application.CommonQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "공통·인증", description = "약관, 지역, 견종 기준정보")
@Validated
@RestController
public class CommonController {

    private final CommonQueryService commonQueryService;

    public CommonController(CommonQueryService commonQueryService) {
        this.commonQueryService = commonQueryService;
    }

    @Operation(summary = "이용약관 목록 조회")
    @GetMapping("/api/v1/terms")
    public TermsResponse terms(@RequestParam @NotBlank String role) {
        return commonQueryService.terms(role);
    }

    @Operation(summary = "지역 목록 조회")
    @GetMapping("/api/v1/regions")
    public RegionsResponse regions(
            @RequestParam(required = false) @Positive Long parentRegionId) {
        return commonQueryService.regions(parentRegionId);
    }

    @Operation(summary = "견종 목록 조회")
    @GetMapping("/api/v1/breeds")
    public BreedsResponse breeds(
            @RequestParam(required = false) @Size(max = 100) String keyword) {
        return commonQueryService.breeds(keyword);
    }
}
