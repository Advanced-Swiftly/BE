package com.advancedswiftly.pawfit.admin.api;

import com.advancedswiftly.pawfit.admin.api.dto.AdminConditionRequestDetailResponse;
import com.advancedswiftly.pawfit.admin.api.dto.ConditionReviewRequest;
import com.advancedswiftly.pawfit.admin.api.dto.ConditionReviewResponse;
import com.advancedswiftly.pawfit.admin.api.dto.PolicyChangeRequest;
import com.advancedswiftly.pawfit.admin.api.dto.PolicyChangeResponse;
import com.advancedswiftly.pawfit.admin.api.dto.ConditionReviewListResponse;
import com.advancedswiftly.pawfit.admin.application.AdminConditionService;
import com.advancedswiftly.pawfit.admin.application.AdminListService;
import com.advancedswiftly.pawfit.business.domain.ConditionRequestKind;
import com.advancedswiftly.pawfit.business.domain.ConditionRequestStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "운영자 심사", description = "운영자의 입장 조건 요청 심사와 정책 직접 변경")
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('OPERATOR')")
public class AdminConditionController {

    private final AdminConditionService conditionService;
    private final AdminListService listService;

    public AdminConditionController(AdminConditionService conditionService, AdminListService listService) {
        this.conditionService = conditionService;
        this.listService = listService;
    }

    @Operation(
            summary = "입장 조건 심사 목록 조회",
            description = "업체 요청·AI 후보·운영자 직접 변경·재검토 요청을 종류와 상태별로 최신순 20건씩 조회합니다. "
                    + "status의 NEEDS_INFO는 보완 요청 상태(NEEDS_SUPPLEMENT)와 같은 뜻으로 받습니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(responseCode = "400", description = "필터 또는 page 값 오류")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "403", description = "운영자 권한 필요")
    @GetMapping("/condition-requests")
    public ConditionReviewListResponse listConditionRequests(
            @Parameter(
                    description = "요청 종류",
                    schema = @Schema(
                            type = "string",
                            allowableValues = {"CONDITION_REQUEST", "AI_CANDIDATE", "OPERATOR_DIRECT", "RECHECK"}))
            @RequestParam(required = false) String kind,
            @Parameter(description = "처리 상태") @RequestParam(required = false) String status,
            @Parameter(description = "업체 ID", example = "100") @RequestParam(required = false) String storeId,
            @Parameter(description = "페이지 번호 (1부터)", example = "1") @RequestParam(required = false) String page) {
        String normalized = status != null && "NEEDS_INFO".equals(status.trim())
                ? ConditionRequestStatus.NEEDS_SUPPLEMENT.name()
                : status;
        return listService.listConditionRequests(
                AdminControllerSupport.optionalEnum(ConditionRequestKind.class, "kind", kind),
                AdminControllerSupport.optionalEnum(ConditionRequestStatus.class, "status", normalized),
                AdminControllerSupport.optionalLong("storeId", storeId),
                AdminControllerSupport.parsePage(page));
    }

    @Operation(summary = "입장 조건 심사 상세 조회", description = "요청한 정책, 현재 정책, 두 정책의 차이와 근거 파일을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "403", description = "운영자 권한 필요")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 요청")
    @GetMapping("/condition-requests/{requestId}")
    public AdminConditionRequestDetailResponse getConditionRequest(
            @Parameter(description = "요청 ID", example = "1301") @PathVariable Long requestId) {
        return conditionService.get(requestId);
    }

    @Operation(
            summary = "입장 조건 심사 처리",
            description = "START_REVIEW(접수→심사 중), NEEDS_INFO(→보완 요청, 사유 필수), APPROVE(→승인 후 새 정책 버전 적용), "
                    + "REJECT(→반려, 사유 필수). 승인 시 요청의 기준 버전이 현재 정책 버전과 다르면 POLICY_VERSION_CONFLICT(409)입니다. "
                    + "상태 전이가 맞지 않으면 INVALID_REQUEST_STATE(400)입니다.")
    @ApiResponse(responseCode = "200", description = "처리 성공")
    @ApiResponse(responseCode = "400", description = "요청 값 오류 또는 INVALID_REQUEST_STATE")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "403", description = "운영자 권한 필요")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 요청")
    @ApiResponse(responseCode = "409", description = "POLICY_VERSION_CONFLICT")
    @PatchMapping("/condition-requests/{requestId}")
    public ConditionReviewResponse reviewConditionRequest(
            Authentication authentication,
            @Parameter(description = "요청 ID", example = "1301") @PathVariable Long requestId,
            @Valid @RequestBody ConditionReviewRequest request) {
        return conditionService.review(AdminControllerSupport.currentUserId(authentication), requestId, request);
    }

    @Operation(
            summary = "운영자 정책 직접 변경",
            description = "업체 요청 없이 운영자가 정책을 바로 바꿉니다. 변경 사유가 OPERATOR_DIRECT 요청으로 남고 새 정책 버전이 적용됩니다. "
                    + "기준 버전이 현재 버전과 다르면 POLICY_VERSION_CONFLICT(409)입니다.")
    @ApiResponse(responseCode = "201", description = "변경 성공")
    @ApiResponse(responseCode = "400", description = "요청 값 오류")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "403", description = "운영자 권한 필요")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 업체")
    @ApiResponse(responseCode = "409", description = "POLICY_VERSION_CONFLICT")
    @PostMapping("/stores/{storeId}/policy-changes")
    @ResponseStatus(HttpStatus.CREATED)
    public PolicyChangeResponse changePolicy(
            Authentication authentication,
            @Parameter(description = "업체 ID", example = "100") @PathVariable Long storeId,
            @Valid @RequestBody PolicyChangeRequest request) {
        return conditionService.changePolicy(AdminControllerSupport.currentUserId(authentication), storeId, request);
    }
}
