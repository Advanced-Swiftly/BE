package com.advancedswiftly.pawfit.admin.api;

import com.advancedswiftly.pawfit.admin.api.dto.AdminVerificationDetailResponse;
import com.advancedswiftly.pawfit.admin.api.dto.AdminVerificationListResponse;
import com.advancedswiftly.pawfit.admin.api.dto.VerificationReviewRequest;
import com.advancedswiftly.pawfit.admin.api.dto.VerificationReviewResponse;
import com.advancedswiftly.pawfit.admin.application.AdminVerificationService;
import com.advancedswiftly.pawfit.business.domain.VerificationStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.util.StringUtils;

@Tag(name = "운영자 심사", description = "운영자의 사업자 인증 심사")
@RestController
@RequestMapping("/api/v1/admin/business-verifications")
@PreAuthorize("hasRole('OPERATOR')")
public class AdminVerificationController {

    private final AdminVerificationService verificationService;

    public AdminVerificationController(AdminVerificationService verificationService) {
        this.verificationService = verificationService;
    }

    @Operation(
            summary = "사업자 인증 심사 목록 조회",
            description = "상태·키워드(상호명 또는 사업자번호)로 최신순 20건씩 조회합니다. "
                    + "status의 NEEDS_INFO는 보완 요청 상태(NEEDS_SUPPLEMENT)와 같은 뜻으로 받습니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(responseCode = "400", description = "status 또는 page 값 오류")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "403", description = "운영자 권한 필요")
    @GetMapping
    public AdminVerificationListResponse list(
            @Parameter(
                    description = "처리 상태",
                    schema = @Schema(
                            type = "string",
                            allowableValues = {"RECEIVED", "IN_REVIEW", "NEEDS_SUPPLEMENT", "NEEDS_INFO", "APPROVED", "REJECTED"}))
            @RequestParam(required = false) String status,
            @Parameter(description = "상호명·사업자번호 검색어", example = "멍카페") @RequestParam(required = false) String keyword,
            @Parameter(description = "페이지 번호 (1부터)", example = "1") @RequestParam(required = false) String page) {
        String normalized = StringUtils.hasText(status) && "NEEDS_INFO".equals(status.trim())
                ? VerificationStatus.NEEDS_SUPPLEMENT.name()
                : status;
        return verificationService.list(
                AdminControllerSupport.optionalEnum(VerificationStatus.class, "status", normalized),
                keyword,
                AdminControllerSupport.parsePage(page));
    }

    @Operation(summary = "사업자 인증 심사 상세 조회", description = "제출 문서·업체 정보와 상태 이력(접수부터 오래된 순)을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "403", description = "운영자 권한 필요")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 인증 요청")
    @GetMapping("/{verificationId}")
    public AdminVerificationDetailResponse get(
            @Parameter(description = "인증 요청 ID", example = "1101") @PathVariable Long verificationId) {
        return verificationService.get(verificationId);
    }

    @Operation(
            summary = "사업자 인증 심사 처리",
            description = "START_REVIEW(접수→심사 중), NEEDS_INFO(→보완 요청, 사유 필수), APPROVE(→승인, 업체 인증 완료와 "
                    + "관리자 연결), REJECT(→반려, 사유 필수). 허용되지 않는 전이는 INVALID_STATUS_TRANSITION(409)입니다.")
    @ApiResponse(responseCode = "200", description = "처리 성공")
    @ApiResponse(responseCode = "400", description = "요청 값 오류")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "403", description = "운영자 권한 필요")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 인증 요청")
    @ApiResponse(responseCode = "409", description = "INVALID_STATUS_TRANSITION")
    @PatchMapping("/{verificationId}")
    public VerificationReviewResponse review(
            Authentication authentication,
            @Parameter(description = "인증 요청 ID", example = "1101") @PathVariable Long verificationId,
            @Valid @RequestBody VerificationReviewRequest request) {
        return verificationService.review(AdminControllerSupport.currentUserId(authentication), verificationId, request);
    }
}
