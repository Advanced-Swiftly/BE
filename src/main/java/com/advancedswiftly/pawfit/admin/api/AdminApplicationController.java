package com.advancedswiftly.pawfit.admin.api;

import com.advancedswiftly.pawfit.admin.api.dto.AdminAdApplicationDetailResponse;
import com.advancedswiftly.pawfit.admin.api.dto.ApplicationReviewRequest;
import com.advancedswiftly.pawfit.admin.api.dto.ApplicationReviewResponse;
import com.advancedswiftly.pawfit.admin.application.AdminApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "운영자 심사", description = "운영자의 광고·이벤트 참여 신청 심사")
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('OPERATOR')")
public class AdminApplicationController {

    private final AdminApplicationService applicationService;

    public AdminApplicationController(AdminApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @Operation(summary = "광고 신청 심사 상세 조회", description = "업체·노출 기간·광고 소재와 심사 상태·의견을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "403", description = "운영자 권한 필요")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 신청")
    @GetMapping("/ad-applications/{applicationId}")
    public AdminAdApplicationDetailResponse getAdApplication(
            @Parameter(description = "광고 신청 ID", example = "1601") @PathVariable Long applicationId) {
        return applicationService.getAd(applicationId);
    }

    @Operation(
            summary = "광고 신청 심사 처리",
            description = "START_REVIEW(접수→심사 중), NEEDS_INFO(심사 중→보완 요청), APPROVE(심사 중→승인), "
                    + "REJECT(심사 중→반려). NEEDS_INFO, REJECT는 comment가 필수입니다. 허용되지 않는 전이는 INVALID_REQUEST_STATE(400)입니다.")
    @ApiResponse(responseCode = "200", description = "처리 성공")
    @ApiResponse(responseCode = "400", description = "요청 값 오류 또는 INVALID_REQUEST_STATE")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "403", description = "운영자 권한 필요")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 신청")
    @PatchMapping("/ad-applications/{applicationId}")
    public ApplicationReviewResponse reviewAdApplication(
            Authentication authentication,
            @Parameter(description = "광고 신청 ID", example = "1601") @PathVariable Long applicationId,
            @Valid @RequestBody ApplicationReviewRequest request) {
        return applicationService.reviewAd(AdminControllerSupport.currentUserId(authentication), applicationId, request);
    }

    @Operation(
            summary = "이벤트 참여 신청 심사 처리",
            description = "광고 신청과 같은 동작·전이 규칙을 씁니다. 승인되면 업체의 QR이 있을 때 이벤트 참여 업체(event_store)로 등록됩니다.")
    @ApiResponse(responseCode = "200", description = "처리 성공")
    @ApiResponse(responseCode = "400", description = "요청 값 오류 또는 INVALID_REQUEST_STATE")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "403", description = "운영자 권한 필요")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 신청")
    @PatchMapping("/event-applications/{applicationId}")
    public ApplicationReviewResponse reviewEventApplication(
            Authentication authentication,
            @Parameter(description = "이벤트 신청 ID", example = "1501") @PathVariable Long applicationId,
            @Valid @RequestBody ApplicationReviewRequest request) {
        return applicationService.reviewEvent(AdminControllerSupport.currentUserId(authentication), applicationId, request);
    }
}
