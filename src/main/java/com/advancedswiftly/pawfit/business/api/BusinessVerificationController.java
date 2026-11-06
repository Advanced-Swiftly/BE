package com.advancedswiftly.pawfit.business.api;

import com.advancedswiftly.pawfit.business.api.dto.VerificationCreateRequest;
import com.advancedswiftly.pawfit.business.api.dto.VerificationCreateResponse;
import com.advancedswiftly.pawfit.business.application.BusinessVerificationService;
import com.advancedswiftly.pawfit.global.error.BusinessException;
import com.advancedswiftly.pawfit.global.error.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "업체 인증", description = "업체 관리자의 사업자 인증 요청·조회·보완")
@RestController
@RequestMapping("/api/v1/business/verifications")
public class BusinessVerificationController {

    private final BusinessVerificationService verificationService;

    public BusinessVerificationController(BusinessVerificationService verificationService) {
        this.verificationService = verificationService;
    }

    @Operation(
            summary = "사업자 인증 요청",
            description = "사업자등록증과 업체 정보를 제출해 업체관리 권한을 요청합니다. "
                    + "반려되지 않은 요청이 이미 있는 사업자등록번호는 다시 요청할 수 없습니다.")
    @ApiResponse(responseCode = "201", description = "요청 접수")
    @ApiResponse(responseCode = "400", description = "요청 값 오류 또는 DUPLICATE_BUSINESS_NUMBER")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "403", description = "업체 관리자 권한 필요")
    @PreAuthorize("hasRole('BUSINESS')")
    @PostMapping
    public ResponseEntity<VerificationCreateResponse> requestVerification(
            Authentication authentication, @Valid @RequestBody VerificationCreateRequest request) {
        VerificationCreateResponse response =
                verificationService.requestVerification(currentUserId(authentication), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** 인증 필터가 Authentication의 name에 user_id를 담는다고 가정한다. */
    private Long currentUserId(Authentication authentication) {
        try {
            return Long.valueOf(authentication.getName());
        } catch (NumberFormatException exception) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }
}
