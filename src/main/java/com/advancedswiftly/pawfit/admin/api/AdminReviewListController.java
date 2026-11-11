package com.advancedswiftly.pawfit.admin.api;

import com.advancedswiftly.pawfit.admin.api.dto.AdminAdApplicationListResponse;
import com.advancedswiftly.pawfit.admin.api.dto.AdminEventApplicationListResponse;
import com.advancedswiftly.pawfit.admin.api.dto.VaccinationReviewActionRequest;
import com.advancedswiftly.pawfit.admin.api.dto.VaccinationReviewActionResponse;
import com.advancedswiftly.pawfit.admin.api.dto.VaccinationReviewListResponse;
import com.advancedswiftly.pawfit.admin.application.AdminListService;
import com.advancedswiftly.pawfit.admin.application.AdminVaccinationService;
import com.advancedswiftly.pawfit.business.domain.AdType;
import com.advancedswiftly.pawfit.business.domain.ApplicationStatus;
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

@Tag(name = "운영자 심사", description = "운영자의 광고·이벤트 신청 목록과 예방접종 검토")
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('OPERATOR')")
public class AdminReviewListController {

    private static final String APPLICATION_STATUSES =
            "RECEIVED,IN_REVIEW,NEEDS_INFO,APPROVED,REJECTED,CANCELED";

    private final AdminListService listService;
    private final AdminVaccinationService vaccinationService;

    public AdminReviewListController(AdminListService listService, AdminVaccinationService vaccinationService) {
        this.listService = listService;
        this.vaccinationService = vaccinationService;
    }

    @Operation(summary = "광고 신청 목록 조회", description = "최신순 20건씩. from/to는 광고 노출 기간과 겹치는 신청을 찾습니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(responseCode = "400", description = "필터 또는 page 값 오류")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "403", description = "운영자 권한 필요")
    @GetMapping("/ad-applications")
    public AdminAdApplicationListResponse listAdApplications(
            @Parameter(description = "처리 상태", schema = @Schema(type = "string")) @RequestParam(required = false) String status,
            @Parameter(description = "광고 유형") @RequestParam(required = false) String adType,
            @Parameter(description = "노출 기간 시작 (yyyy-MM-dd)") @RequestParam(required = false) String from,
            @Parameter(description = "노출 기간 종료 (yyyy-MM-dd)") @RequestParam(required = false) String to,
            @Parameter(description = "페이지 번호 (1부터)", example = "1") @RequestParam(required = false) String page) {
        return listService.listAds(
                AdminControllerSupport.optionalEnum(ApplicationStatus.class, "status", status),
                AdminControllerSupport.optionalEnum(AdType.class, "adType", adType),
                AdminControllerSupport.optionalDate("from", from),
                AdminControllerSupport.optionalDate("to", to),
                AdminControllerSupport.parsePage(page));
    }

    @Operation(summary = "이벤트 참여 신청 목록 조회", description = "최신순 20건씩. 이벤트·지역·상태로 거를 수 있습니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(responseCode = "400", description = "필터 또는 page 값 오류")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "403", description = "운영자 권한 필요")
    @GetMapping("/event-applications")
    public AdminEventApplicationListResponse listEventApplications(
            @Parameter(description = "이벤트 ID", example = "801") @RequestParam(required = false) String eventId,
            @Parameter(description = "지역 ID", example = "11") @RequestParam(required = false) String regionId,
            @Parameter(description = "처리 상태 (" + APPLICATION_STATUSES + ")") @RequestParam(required = false) String status,
            @Parameter(description = "페이지 번호 (1부터)", example = "1") @RequestParam(required = false) String page) {
        return listService.listEventApplications(
                AdminControllerSupport.optionalLong("eventId", eventId),
                AdminControllerSupport.optionalLong("regionId", regionId),
                AdminControllerSupport.optionalEnum(ApplicationStatus.class, "status", status),
                AdminControllerSupport.parsePage(page));
    }

    @Operation(summary = "예방접종 검토 목록 조회", description = "검토가 필요한 기록을 최신순 20건씩 조회합니다. status를 생략하면 모든 검토 대상입니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(responseCode = "400", description = "status 또는 page 값 오류")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "403", description = "운영자 권한 필요")
    @GetMapping("/vaccination-records")
    public VaccinationReviewListResponse listVaccinationRecords(
            @Parameter(
                    description = "검토 상태",
                    schema = @Schema(type = "string", allowableValues = {"ANALYZED", "NEEDS_MANUAL_REVIEW", "OPERATOR_REVIEW"}))
            @RequestParam(required = false) String status,
            @Parameter(description = "페이지 번호 (1부터)", example = "1") @RequestParam(required = false) String page) {
        return vaccinationService.list(
                status == null || status.isBlank() ? null : status.trim(), AdminControllerSupport.parsePage(page));
    }

    @Operation(
            summary = "예방접종 검토 처리",
            description = "VERIFY(인증 완료, 접종 항목 전달 가능), NEEDS_MORE(추가 정보 요청, 사유 필수), "
                    + "REQUEST_REUPLOAD(재업로드 요청, 사유 필수). 검토 대상이 아닌 기록은 INVALID_REQUEST_STATE(400)입니다.")
    @ApiResponse(responseCode = "200", description = "처리 성공")
    @ApiResponse(responseCode = "400", description = "요청 값 오류 또는 INVALID_REQUEST_STATE")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "403", description = "운영자 권한 필요")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 기록")
    @PatchMapping("/vaccination-records/{recordId}")
    public VaccinationReviewActionResponse processVaccination(
            Authentication authentication,
            @Parameter(description = "예방접종 기록 ID", example = "20") @PathVariable Long recordId,
            @Valid @RequestBody VaccinationReviewActionRequest request) {
        return vaccinationService.act(AdminControllerSupport.currentUserId(authentication), recordId, request);
    }
}
