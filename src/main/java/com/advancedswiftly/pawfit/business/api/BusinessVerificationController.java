package com.advancedswiftly.pawfit.business.api;

import com.advancedswiftly.pawfit.business.api.dto.VerificationCreateRequest;
import com.advancedswiftly.pawfit.business.api.dto.VerificationCreateResponse;
import com.advancedswiftly.pawfit.business.api.dto.VerificationDetailResponse;
import com.advancedswiftly.pawfit.business.api.dto.VerificationListResponse;
import com.advancedswiftly.pawfit.business.api.dto.VerificationResubmitRequest;
import com.advancedswiftly.pawfit.business.application.BusinessVerificationService;
import com.advancedswiftly.pawfit.business.domain.VerificationStatus;
import com.advancedswiftly.pawfit.global.error.BusinessException;
import com.advancedswiftly.pawfit.global.error.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @Operation(
            summary = "내 사업자 인증 목록 조회",
            description = "업체관리자가 제출한 사업자 인증 요청과 상태를 최신순으로 조회합니다. "
                    + "status를 주면 해당 심사 상태의 요청만 반환합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(responseCode = "400", description = "허용되지 않는 status 값")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "403", description = "업체 관리자 권한 필요")
    @PreAuthorize("hasRole('BUSINESS')")
    @GetMapping
    public VerificationListResponse getMyVerifications(
            Authentication authentication,
            @Parameter(
                    description = "심사 상태 필터",
                    schema = @Schema(
                            type = "string",
                            allowableValues = {
                                "RECEIVED", "IN_REVIEW", "NEEDS_SUPPLEMENT", "APPROVED", "REJECTED"
                            }))
            @RequestParam(required = false) String status) {
        return verificationService.getMyVerifications(
                currentUserId(authentication), parseStatus(status));
    }

    @Operation(
            summary = "사업자 인증 상세 조회",
            description = "본인이 제출한 사업자 인증 건의 처리 상태와 심사 의견(보완 요청·승인·반려 사유)을 조회합니다. "
                    + "심사 전에는 reviewComment 필드가 응답에서 빠집니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(responseCode = "400", description = "요청 값 오류")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "403", description = "업체 관리자 권한 필요")
    @ApiResponse(responseCode = "404", description = "존재하지 않거나 본인의 인증 건이 아님")
    @PreAuthorize("hasRole('BUSINESS')")
    @GetMapping("/{verificationId}")
    public VerificationDetailResponse getMyVerification(
            Authentication authentication,
            @Parameter(description = "사업자 인증 ID", example = "1101") @PathVariable Long verificationId) {
        return verificationService.getMyVerification(currentUserId(authentication), verificationId);
    }

    @Operation(
            summary = "사업자 인증 보완 제출",
            description = "보완 요청(NEEDS_SUPPLEMENT)을 받은 본인의 인증 건에 새 사업자등록증과 "
                    + "수정한 업체 정보를 다시 제출합니다. 제출하면 상태가 RECEIVED로 돌아가 다시 심사됩니다. "
                    + "store는 전달한 항목만 수정합니다.")
    @ApiResponse(responseCode = "200", description = "재제출 접수")
    @ApiResponse(responseCode = "400", description = "요청 값 오류 또는 RESUBMIT_NOT_ALLOWED(보완 요청 상태가 아님)")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "403", description = "업체 관리자 권한 필요")
    @ApiResponse(responseCode = "404", description = "존재하지 않거나 본인의 인증 건이 아님")
    @PreAuthorize("hasRole('BUSINESS')")
    @PostMapping("/{verificationId}/resubmit")
    public VerificationCreateResponse resubmit(
            Authentication authentication,
            @Parameter(description = "사업자 인증 ID", example = "1101") @PathVariable Long verificationId,
            @Valid @RequestBody VerificationResubmitRequest request) {
        return verificationService.resubmit(currentUserId(authentication), verificationId, request);
    }

    /** status 파라미터가 비어 있으면 필터 없음, 허용되지 않는 값이면 400으로 응답한다. */
    private VerificationStatus parseStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        try {
            return VerificationStatus.valueOf(status.trim());
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "status 값이 올바르지 않습니다. (RECEIVED, IN_REVIEW, NEEDS_SUPPLEMENT, APPROVED, REJECTED)");
        }
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
