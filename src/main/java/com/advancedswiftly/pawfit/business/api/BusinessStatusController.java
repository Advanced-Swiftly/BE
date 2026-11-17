package com.advancedswiftly.pawfit.business.api;

import com.advancedswiftly.pawfit.business.application.BusinessStatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "업체 인증", description = "국세청 사업자등록 상태조회")
@RestController
@RequestMapping("/api/v1/business/verifications/business-status")
@PreAuthorize("hasRole('BUSINESS')")
public class BusinessStatusController {

    private final BusinessStatusService service;

    public BusinessStatusController(BusinessStatusService service) {
        this.service = service;
    }

    @Operation(summary = "사업자등록 상태조회", description = "국세청 계속사업자는 isActive=true, 휴업·폐업·미등록은 false입니다. 업체 심사 승인을 의미하지 않습니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 사업자등록번호")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "403", description = "업체 관리자 권한 필요")
    @ApiResponse(responseCode = "503", description = "인증키 미설정 또는 국세청 서비스 장애")
    @PostMapping
    public BusinessStatusResponse lookup(@Valid @RequestBody BusinessStatusRequest request) {
        return new BusinessStatusResponse(service.isActive(request.businessNumber()));
    }

    public record BusinessStatusRequest(
            @Schema(description = "10자리 사업자등록번호", example = "123-45-67890")
            @NotBlank @Pattern(regexp = "^\\d{3}-?\\d{2}-?\\d{5}$") String businessNumber) {
    }

    public record BusinessStatusResponse(boolean isActive) {
    }
}
