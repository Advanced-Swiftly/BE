package com.advancedswiftly.pawfit.admin.api;

import com.advancedswiftly.pawfit.admin.api.dto.DashboardSummaryResponse;
import com.advancedswiftly.pawfit.admin.api.dto.PolicyHistoryResponse;
import com.advancedswiftly.pawfit.admin.api.dto.VaccinationReviewResponse;
import com.advancedswiftly.pawfit.admin.application.AdminOverviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "운영자 심사", description = "운영자의 대시보드·이력·심사 조회")
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('OPERATOR')")
public class AdminOverviewController {

    private final AdminOverviewService overviewService;

    public AdminOverviewController(AdminOverviewService overviewService) {
        this.overviewService = overviewService;
    }

    @Operation(
            summary = "운영자 대시보드 요약 조회",
            description = "심사 대기 건수, 인증 완료 업체 수, 방문 인증 수를 조회합니다. from/to(yyyy-MM-dd, UTC)는 함께 "
                    + "전달해야 하며 방문 인증 수에만 적용됩니다. 생략하면 전체 기간입니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(responseCode = "400", description = "날짜 형식 오류 또는 from/to 불일치")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "403", description = "운영자 권한 필요")
    @GetMapping("/dashboard/summary")
    public DashboardSummaryResponse dashboardSummary(
            @Parameter(description = "집계 시작일 (yyyy-MM-dd)", example = "2026-11-01") @RequestParam(required = false) String from,
            @Parameter(description = "집계 종료일 (yyyy-MM-dd)", example = "2026-11-30") @RequestParam(required = false) String to) {
        return overviewService.dashboardSummary(
                AdminControllerSupport.optionalDate("from", from), AdminControllerSupport.optionalDate("to", to));
    }

    @Operation(summary = "업체 정책 변경 이력 조회", description = "업체 정책의 버전별 원문·변경 주체·승인된 요청을 버전 내림차순으로 20건씩 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(responseCode = "400", description = "page 값 오류")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "403", description = "운영자 권한 필요")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 업체")
    @GetMapping("/stores/{storeId}/policy-history")
    public PolicyHistoryResponse policyHistory(
            @Parameter(description = "업체 ID", example = "100") @PathVariable Long storeId,
            @Parameter(description = "페이지 번호 (1부터)", example = "1") @RequestParam(required = false) String page) {
        return overviewService.policyHistory(storeId, AdminControllerSupport.parsePage(page));
    }

    @Operation(summary = "예방접종 검토 상세 조회", description = "증명서 문서와 접종 항목(AI 추출은 신뢰도 포함)을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "403", description = "운영자 권한 필요")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 기록")
    @GetMapping("/vaccination-records/{recordId}")
    public VaccinationReviewResponse vaccinationReview(
            @Parameter(description = "예방접종 기록 ID", example = "20") @PathVariable Long recordId) {
        return overviewService.vaccinationReview(recordId);
    }
}
