package com.advancedswiftly.pawfit.business.api;

import com.advancedswiftly.pawfit.business.api.dto.BusinessHoursResponse;
import com.advancedswiftly.pawfit.business.api.dto.BusinessHoursUpdateRequest;
import com.advancedswiftly.pawfit.business.api.dto.BusinessHoursUpdateResponse;
import com.advancedswiftly.pawfit.business.api.dto.ConditionRequestActionRequest;
import com.advancedswiftly.pawfit.business.api.dto.ConditionRequestCreateRequest;
import com.advancedswiftly.pawfit.business.api.dto.ConditionRequestListResponse;
import com.advancedswiftly.pawfit.business.api.dto.ManagedStoreListResponse;
import com.advancedswiftly.pawfit.business.api.dto.QrIssueRequest;
import com.advancedswiftly.pawfit.business.api.dto.QrIssueResponse;
import com.advancedswiftly.pawfit.business.api.dto.StoreDetailResponse;
import com.advancedswiftly.pawfit.business.api.dto.ConditionRequestDetailResponse;
import com.advancedswiftly.pawfit.business.api.dto.ConditionRequestResultResponse;
import com.advancedswiftly.pawfit.business.api.dto.EventStatsResponse;
import com.advancedswiftly.pawfit.business.api.dto.QrActiveRequest;
import com.advancedswiftly.pawfit.business.api.dto.StorePolicyResponse;
import com.advancedswiftly.pawfit.business.api.dto.StoreProfileUpdateRequest;
import com.advancedswiftly.pawfit.business.api.dto.StoreProfileUpdateResponse;
import com.advancedswiftly.pawfit.business.api.dto.StoreQrResponse;
import com.advancedswiftly.pawfit.business.api.dto.VisitStatsResponse;
import com.advancedswiftly.pawfit.business.application.BusinessHoursService;
import com.advancedswiftly.pawfit.business.application.ConditionRequestService;
import com.advancedswiftly.pawfit.business.domain.ConditionRequestStatus;
import com.advancedswiftly.pawfit.business.application.StoreOperationService;
import com.advancedswiftly.pawfit.business.application.StoreStatsService;
import com.advancedswiftly.pawfit.business.application.StoreStatsService.GroupBy;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "업체 운영", description = "업체 관리자의 업체 정보·입장 정책·QR·통계 관리")
@RestController
@RequestMapping("/api/v1/business")
@PreAuthorize("hasRole('BUSINESS')")
public class StoreOperationController {

    private final StoreOperationService storeOperationService;
    private final ConditionRequestService conditionRequestService;
    private final StoreStatsService storeStatsService;
    private final BusinessHoursService businessHoursService;

    public StoreOperationController(
            StoreOperationService storeOperationService,
            ConditionRequestService conditionRequestService,
            StoreStatsService storeStatsService,
            BusinessHoursService businessHoursService) {
        this.businessHoursService = businessHoursService;
        this.storeOperationService = storeOperationService;
        this.conditionRequestService = conditionRequestService;
        this.storeStatsService = storeStatsService;
    }

    @Operation(
            summary = "업체 기본 정보 수정",
            description = "업체명·연락처·소개·편의시설처럼 즉시 수정할 수 있는 정보를 변경합니다. 전달한 항목만 수정하며, "
                    + "amenities를 전달하면 목록 전체를 교체합니다.")
    @ApiResponse(responseCode = "200", description = "수정 성공")
    @ApiResponse(responseCode = "400", description = "요청 값 오류")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "403", description = "업체 관리자 권한 없음 또는 관리 업체가 아님")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 업체")
    @PatchMapping("/stores/{storeId}")
    public StoreProfileUpdateResponse updateStore(
            Authentication authentication,
            @Parameter(description = "업체 ID", example = "100") @PathVariable Long storeId,
            @Valid @RequestBody StoreProfileUpdateRequest request) {
        return storeOperationService.updateProfile(
                BusinessControllerSupport.currentUserId(authentication), storeId, request);
    }

    @Operation(summary = "현재 입장 정책 조회", description = "업체에 적용 중인 정책 버전과 견종·장비·접종 조건을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "403", description = "업체 관리자 권한 없음 또는 관리 업체가 아님")
    @ApiResponse(responseCode = "404", description = "적용 중인 정책이 없음")
    @GetMapping("/stores/{storeId}/policy")
    public StorePolicyResponse getPolicy(
            Authentication authentication,
            @Parameter(description = "업체 ID", example = "100") @PathVariable Long storeId) {
        return storeOperationService.getCurrentPolicy(
                BusinessControllerSupport.currentUserId(authentication), storeId);
    }

    @Operation(
            summary = "입장 조건 변경 요청",
            description = "현재 정책 버전을 기준으로 조건 변경안과 근거를 제출합니다. 기준 버전(baseVersion)이 현재 정책 "
                    + "버전과 다르면 POLICY_VERSION_CONFLICT(409)를 반환합니다. 정책이 아직 없으면 baseVersion은 0입니다.")
    @ApiResponse(responseCode = "201", description = "요청 접수")
    @ApiResponse(responseCode = "400", description = "요청 값 오류")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "403", description = "업체 관리자 권한 없음 또는 관리 업체가 아님")
    @ApiResponse(responseCode = "409", description = "POLICY_VERSION_CONFLICT (그 사이 정책이 변경됨)")
    @PostMapping("/stores/{storeId}/condition-requests")
    public ResponseEntity<ConditionRequestResultResponse> requestConditionChange(
            Authentication authentication,
            @Parameter(description = "업체 ID", example = "100") @PathVariable Long storeId,
            @Valid @RequestBody ConditionRequestCreateRequest request) {
        ConditionRequestResultResponse response = conditionRequestService.create(
                BusinessControllerSupport.currentUserId(authentication), storeId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "입장 조건 요청 상세 조회", description = "본인이 제출한 변경 요청의 처리 상태와 심사 의견을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "403", description = "업체 관리자 권한 필요")
    @ApiResponse(responseCode = "404", description = "존재하지 않거나 본인의 요청이 아님")
    @GetMapping("/condition-requests/{requestId}")
    public ConditionRequestDetailResponse getConditionRequest(
            Authentication authentication,
            @Parameter(description = "조건 요청 ID", example = "1301") @PathVariable Long requestId) {
        return conditionRequestService.get(BusinessControllerSupport.currentUserId(authentication), requestId);
    }

    @Operation(summary = "업체 QR 조회", description = "현재 유효한 업체 QR의 상태와 표시용 이미지를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "403", description = "업체 관리자 권한 없음 또는 관리 업체가 아님")
    @ApiResponse(responseCode = "404", description = "발급된 QR이 없음")
    @GetMapping("/stores/{storeId}/qr")
    public StoreQrResponse getQr(
            Authentication authentication,
            @Parameter(description = "업체 ID", example = "100") @PathVariable Long storeId) {
        return storeOperationService.getQr(BusinessControllerSupport.currentUserId(authentication), storeId);
    }

    @Operation(summary = "업체 QR 활성 상태 변경", description = "현재 QR을 비활성화하거나 다시 활성화합니다.")
    @ApiResponse(responseCode = "200", description = "변경 성공")
    @ApiResponse(responseCode = "400", description = "요청 값 오류")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "403", description = "업체 관리자 권한 없음 또는 관리 업체가 아님")
    @ApiResponse(responseCode = "404", description = "발급된 QR이 없음")
    @PatchMapping("/stores/{storeId}/qr")
    public StoreQrResponse changeQrActive(
            Authentication authentication,
            @Parameter(description = "업체 ID", example = "100") @PathVariable Long storeId,
            @Valid @RequestBody QrActiveRequest request) {
        return storeOperationService.changeQrActive(
                BusinessControllerSupport.currentUserId(authentication), storeId, request.active());
    }

    @Operation(
            summary = "업체 방문 인증 통계 조회",
            description = "기간별 QR 인증 방문 수와 스탬프 지급 수를 조회합니다. 날짜 경계는 UTC 기준이고, 기간은 최대 366일입니다. "
                    + "series에는 방문이 있는 구간만 날짜 오름차순으로 담깁니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(responseCode = "400", description = "날짜 형식·기간 오류 또는 허용되지 않는 groupBy")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "403", description = "업체 관리자 권한 없음 또는 관리 업체가 아님")
    @GetMapping("/stores/{storeId}/visit-stats")
    public VisitStatsResponse getVisitStats(
            Authentication authentication,
            @Parameter(description = "업체 ID", example = "100") @PathVariable Long storeId,
            @Parameter(description = "조회 시작일 (yyyy-MM-dd)", example = "2026-11-01") @RequestParam(required = false) String from,
            @Parameter(description = "조회 종료일 (yyyy-MM-dd)", example = "2026-11-30") @RequestParam(required = false) String to,
            @Parameter(
                    description = "집계 단위. 생략하면 DAY",
                    schema = @Schema(type = "string", allowableValues = {"DAY", "WEEK", "MONTH"}))
            @RequestParam(required = false) String groupBy) {
        return storeStatsService.getVisitStats(
                BusinessControllerSupport.currentUserId(authentication),
                storeId,
                BusinessControllerSupport.requireDate("from", from),
                BusinessControllerSupport.requireDate("to", to),
                parseGroupBy(groupBy));
    }

    @Operation(
            summary = "업체 이벤트 통계 조회",
            description = "관리 업체의 이벤트별 방문·스탬프·응모 전환 통계를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(responseCode = "400", description = "eventId 누락 또는 형식 오류")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "403", description = "업체 관리자 권한 없음 또는 관리 업체가 아님")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 이벤트")
    @GetMapping("/stores/{storeId}/event-stats")
    public EventStatsResponse getEventStats(
            Authentication authentication,
            @Parameter(description = "업체 ID", example = "100") @PathVariable Long storeId,
            @Parameter(description = "이벤트 ID", example = "801") @RequestParam(required = false) String eventId) {
        return storeStatsService.getEventStats(
                BusinessControllerSupport.currentUserId(authentication),
                storeId,
                BusinessControllerSupport.requireLong("eventId", eventId));
    }

    @Operation(summary = "내 관리 업체 목록", description = "인증이 승인되어 관리할 수 있는 업체 목록을 조회합니다. 인증 완료 업체는 VISIBLE, 아니면 HIDDEN입니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "403", description = "업체 관리자 권한 필요")
    @GetMapping("/stores")
    public ManagedStoreListResponse listStores(Authentication authentication) {
        return storeOperationService.listManagedStores(BusinessControllerSupport.currentUserId(authentication));
    }

    @Operation(summary = "관리 업체 상세 조회", description = "업체 기본 정보, 영업시간, 적용 중인 정책 버전과 QR 상태를 조회합니다. 값이 없는 항목은 응답에서 빠집니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "403", description = "업체 관리자 권한 없음 또는 관리 업체가 아님")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 업체")
    @GetMapping("/stores/{storeId}")
    public StoreDetailResponse getStore(
            Authentication authentication,
            @Parameter(description = "업체 ID", example = "100") @PathVariable Long storeId) {
        return storeOperationService.getStore(BusinessControllerSupport.currentUserId(authentication), storeId);
    }

    @Operation(summary = "영업시간 조회", description = "요일별 영업시간을 월요일부터 순서대로 조회합니다. 등록된 값이 없으면 빈 배열입니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "403", description = "업체 관리자 권한 없음 또는 관리 업체가 아님")
    @GetMapping("/stores/{storeId}/business-hours")
    public BusinessHoursResponse getBusinessHours(
            Authentication authentication,
            @Parameter(description = "업체 ID", example = "100") @PathVariable Long storeId) {
        return businessHoursService.get(BusinessControllerSupport.currentUserId(authentication), storeId);
    }

    @Operation(summary = "영업시간 수정", description = "전달한 요일의 영업시간만 변경합니다. 영업하는 요일은 시작·종료 시각(HH:mm)이 필요하고 종료가 시작보다 늦어야 합니다.")
    @ApiResponse(responseCode = "200", description = "수정 성공")
    @ApiResponse(responseCode = "400", description = "요청 값 오류")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "403", description = "업체 관리자 권한 없음 또는 관리 업체가 아님")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 업체")
    @PatchMapping("/stores/{storeId}/business-hours")
    public BusinessHoursUpdateResponse updateBusinessHours(
            Authentication authentication,
            @Parameter(description = "업체 ID", example = "100") @PathVariable Long storeId,
            @Valid @RequestBody BusinessHoursUpdateRequest request) {
        return businessHoursService.update(BusinessControllerSupport.currentUserId(authentication), storeId, request);
    }

    @Operation(summary = "입장 조건 요청 목록 조회", description = "업체의 변경 요청을 최신순으로 20건씩 조회합니다. page는 1부터 시작하고, status로 필터링할 수 있습니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(responseCode = "400", description = "status 또는 page 값 오류")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "403", description = "업체 관리자 권한 없음 또는 관리 업체가 아님")
    @GetMapping("/stores/{storeId}/condition-requests")
    public ConditionRequestListResponse listConditionRequests(
            Authentication authentication,
            @Parameter(description = "업체 ID", example = "100") @PathVariable Long storeId,
            @Parameter(
                    description = "처리 상태 필터",
                    schema = @Schema(type = "string", allowableValues = {
                            "RECEIVED", "IN_REVIEW", "NEEDS_SUPPLEMENT", "APPROVED", "REJECTED", "CANCELED"}))
            @RequestParam(required = false) String status,
            @Parameter(description = "페이지 번호 (1부터)", example = "1") @RequestParam(required = false) String page) {
        return conditionRequestService.list(
                BusinessControllerSupport.currentUserId(authentication),
                storeId,
                parseConditionStatus(status),
                parsePage(page));
    }

    @Operation(
            summary = "입장 조건 요청 취소·보완 제출",
            description = "action이 CANCEL이면 대기 중인 요청을 취소하고, RESUBMIT이면 보완 요청된 건을 보완 내용과 함께 다시 제출합니다. "
                    + "본인 요청이 아니면 404, 현재 상태에서 할 수 없는 동작이면 INVALID_REQUEST_STATE(400)입니다.")
    @ApiResponse(responseCode = "200", description = "처리 성공")
    @ApiResponse(responseCode = "400", description = "요청 값 오류 또는 INVALID_REQUEST_STATE")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "403", description = "업체 관리자 권한 필요")
    @ApiResponse(responseCode = "404", description = "존재하지 않거나 본인의 요청이 아님")
    @PatchMapping("/condition-requests/{requestId}")
    public ConditionRequestResultResponse actOnConditionRequest(
            Authentication authentication,
            @Parameter(description = "조건 요청 ID", example = "1301") @PathVariable Long requestId,
            @Valid @RequestBody ConditionRequestActionRequest request) {
        return conditionRequestService.act(BusinessControllerSupport.currentUserId(authentication), requestId, request);
    }

    @Operation(
            summary = "업체 QR 발급·재발급",
            description = "새 QR을 발급하고 기존 QR은 모두 폐기합니다. 폐기된 QR은 다시 활성화할 수 없습니다. 본문(reason)은 생략할 수 있습니다.")
    @ApiResponse(responseCode = "201", description = "발급 성공")
    @ApiResponse(responseCode = "400", description = "요청 값 오류")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "403", description = "업체 관리자 권한 없음 또는 관리 업체가 아님")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 업체")
    @PostMapping("/stores/{storeId}/qr")
    public ResponseEntity<QrIssueResponse> issueQr(
            Authentication authentication,
            @Parameter(description = "업체 ID", example = "100") @PathVariable Long storeId,
            @Valid @RequestBody(required = false) QrIssueRequest request) {
        QrIssueResponse response = storeOperationService.issueQr(
                BusinessControllerSupport.currentUserId(authentication), storeId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** groupBy가 비어 있으면 DAY, 허용되지 않는 값이면 400으로 응답한다. */
    private GroupBy parseGroupBy(String groupBy) {
        if (!StringUtils.hasText(groupBy)) {
            return GroupBy.DAY;
        }
        try {
            return GroupBy.valueOf(groupBy.trim());
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "groupBy 값이 올바르지 않습니다. (DAY, WEEK, MONTH)");
        }
    }

    private ConditionRequestStatus parseConditionStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        try {
            return ConditionRequestStatus.valueOf(status.trim());
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "status 값이 올바르지 않습니다.");
        }
    }

    /** page가 비어 있으면 1, 숫자가 아니거나 1 미만이면 400. */
    private int parsePage(String page) {
        if (!StringUtils.hasText(page)) {
            return 1;
        }
        try {
            int value = Integer.parseInt(page.trim());
            if (value < 1) {
                throw new NumberFormatException();
            }
            return value;
        } catch (NumberFormatException exception) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "page는 1 이상의 숫자여야 합니다.");
        }
    }
}
