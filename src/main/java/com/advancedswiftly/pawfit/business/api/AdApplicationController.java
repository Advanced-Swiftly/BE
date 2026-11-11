package com.advancedswiftly.pawfit.business.api;

import com.advancedswiftly.pawfit.business.api.dto.AdApplicationCreateRequest;
import com.advancedswiftly.pawfit.business.api.dto.AdApplicationDetailResponse;
import com.advancedswiftly.pawfit.business.api.dto.AdApplicationListResponse;
import com.advancedswiftly.pawfit.business.api.dto.ApplicationCancelRequest;
import com.advancedswiftly.pawfit.business.api.dto.ApplicationResultResponse;
import com.advancedswiftly.pawfit.business.application.AdApplicationService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "업체 신청", description = "업체 관리자의 이벤트 참여·광고 신청")
@RestController
@RequestMapping("/api/v1/business/ad-applications")
@PreAuthorize("hasRole('BUSINESS')")
public class AdApplicationController {

    private final AdApplicationService applicationService;

    public AdApplicationController(AdApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @Operation(summary = "광고 신청", description = "관리 업체의 배너·추천 광고 노출을 신청합니다. 종료일은 시작일보다 빠를 수 없습니다.")
    @ApiResponse(responseCode = "201", description = "신청 접수")
    @ApiResponse(responseCode = "400", description = "요청 값 오류")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "403", description = "업체 관리자 권한 없음 또는 관리 업체가 아님")
    @PostMapping
    public ResponseEntity<ApplicationResultResponse> apply(
            Authentication authentication, @Valid @RequestBody AdApplicationCreateRequest request) {
        ApplicationResultResponse response =
                applicationService.apply(BusinessControllerSupport.currentUserId(authentication), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "광고 신청 목록 조회",
            description = "내 업체의 광고 신청과 심사 상태를 최신순으로 조회합니다. storeId, status로 필터링할 수 있습니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(responseCode = "400", description = "허용되지 않는 status 값")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "403", description = "업체 관리자 권한 필요")
    @GetMapping
    public AdApplicationListResponse list(
            Authentication authentication,
            @Parameter(description = "업체 ID") @RequestParam(required = false) Long storeId,
            @Parameter(
                    description = "심사 상태 필터",
                    schema = @Schema(
                            type = "string",
                            allowableValues = {"RECEIVED", "IN_REVIEW", "APPROVED", "REJECTED", "CANCELED"}))
            @RequestParam(required = false) String status) {
        return applicationService.list(
                BusinessControllerSupport.currentUserId(authentication),
                storeId,
                BusinessControllerSupport.parseStatus(status));
    }

    @Operation(summary = "광고 신청 상세 조회", description = "광고 신청의 처리 상태와 광고 소재 URL을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "403", description = "업체 관리자 권한 필요")
    @ApiResponse(responseCode = "404", description = "존재하지 않거나 본인의 신청이 아님")
    @GetMapping("/{applicationId}")
    public AdApplicationDetailResponse get(
            Authentication authentication,
            @Parameter(description = "광고 신청 ID", example = "1601") @PathVariable Long applicationId) {
        return applicationService.get(BusinessControllerSupport.currentUserId(authentication), applicationId);
    }

    @Operation(
            summary = "광고 신청 취소",
            description = "심사 전(RECEIVED) 광고 신청을 취소합니다. 이미 심사가 시작됐다면 CANCEL_NOT_ALLOWED를 반환합니다.")
    @ApiResponse(responseCode = "200", description = "취소 성공")
    @ApiResponse(responseCode = "400", description = "요청 값 오류 또는 CANCEL_NOT_ALLOWED")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "403", description = "업체 관리자 권한 필요")
    @ApiResponse(responseCode = "404", description = "존재하지 않거나 본인의 신청이 아님")
    @PatchMapping("/{applicationId}")
    public ApplicationResultResponse cancel(
            Authentication authentication,
            @Parameter(description = "광고 신청 ID", example = "1601") @PathVariable Long applicationId,
            @Valid @RequestBody ApplicationCancelRequest request) {
        return applicationService.cancel(BusinessControllerSupport.currentUserId(authentication), applicationId);
    }
}
