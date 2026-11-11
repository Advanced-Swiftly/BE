package com.advancedswiftly.pawfit.admin.api;

import com.advancedswiftly.pawfit.admin.api.dto.AdminStoreCreateRequest;
import com.advancedswiftly.pawfit.admin.api.dto.AdminStoreCreateResponse;
import com.advancedswiftly.pawfit.admin.api.dto.AdminStoreDetailResponse;
import com.advancedswiftly.pawfit.admin.api.dto.AdminStoreListResponse;
import com.advancedswiftly.pawfit.admin.api.dto.AdminStoreUpdateRequest;
import com.advancedswiftly.pawfit.admin.api.dto.AdminStoreUpdateResponse;
import com.advancedswiftly.pawfit.admin.api.dto.StoreRecheckRequest;
import com.advancedswiftly.pawfit.admin.api.dto.StoreRecheckResponse;
import com.advancedswiftly.pawfit.admin.api.dto.StoreVisibilityRequest;
import com.advancedswiftly.pawfit.admin.api.dto.StoreVisibilityResponse;
import com.advancedswiftly.pawfit.admin.application.AdminStoreService;
import com.advancedswiftly.pawfit.business.domain.StoreVisibility;
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

@Tag(name = "운영자 심사", description = "운영자의 업체 관리")
@RestController
@RequestMapping("/api/v1/admin/stores")
@PreAuthorize("hasRole('OPERATOR')")
public class AdminStoreController {

    private final AdminStoreService storeService;

    public AdminStoreController(AdminStoreService storeService) {
        this.storeService = storeService;
    }

    @Operation(summary = "운영자 업체 목록 조회", description = "지역·노출 상태·업체명 검색으로 최신순 20건씩 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(responseCode = "400", description = "필터 또는 page 값 오류")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "403", description = "운영자 권한 필요")
    @GetMapping
    public AdminStoreListResponse list(
            @Parameter(description = "지역 ID", example = "11") @RequestParam(required = false) String regionId,
            @Parameter(description = "노출 상태", schema = @Schema(type = "string", allowableValues = {"VISIBLE", "HIDDEN"}))
            @RequestParam(required = false) String visibility,
            @Parameter(description = "업체명 검색어", example = "멍카페") @RequestParam(required = false) String keyword,
            @Parameter(description = "페이지 번호 (1부터)", example = "1") @RequestParam(required = false) String page) {
        return storeService.list(
                AdminControllerSupport.optionalLong("regionId", regionId),
                AdminControllerSupport.optionalEnum(StoreVisibility.class, "visibility", visibility),
                keyword,
                AdminControllerSupport.parsePage(page));
    }

    @Operation(summary = "운영자 업체 상세 조회", description = "업체 정보와 관리자, 적용 정책 버전, 처리 대기 요청 수, QR 상태를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "403", description = "운영자 권한 필요")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 업체")
    @GetMapping("/{storeId}")
    public AdminStoreDetailResponse get(@Parameter(description = "업체 ID", example = "100") @PathVariable Long storeId) {
        return storeService.get(storeId);
    }

    @Operation(summary = "운영자 업체 직접 등록", description = "업체를 숨김(HIDDEN) 상태로 등록합니다. 존재하지 않는 지역이면 400입니다.")
    @ApiResponse(responseCode = "201", description = "등록 성공")
    @ApiResponse(responseCode = "400", description = "요청 값 오류")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "403", description = "운영자 권한 필요")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminStoreCreateResponse create(@Valid @RequestBody AdminStoreCreateRequest request) {
        return storeService.create(request);
    }

    @Operation(
            summary = "운영자 업체 정보 수정",
            description = "전달한 항목만 바꿉니다. managerUserId는 업체 관리자 역할의 사용자여야 하며 기존 관리자를 대체합니다.")
    @ApiResponse(responseCode = "200", description = "수정 성공")
    @ApiResponse(responseCode = "400", description = "요청 값 오류 또는 관리자로 지정할 수 없는 사용자")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "403", description = "운영자 권한 필요")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 업체")
    @PatchMapping("/{storeId}")
    public AdminStoreUpdateResponse update(
            @Parameter(description = "업체 ID", example = "100") @PathVariable Long storeId,
            @Valid @RequestBody AdminStoreUpdateRequest request) {
        return storeService.update(storeId, request);
    }

    @Operation(summary = "업체 노출 상태 변경", description = "지도·검색에서 업체를 노출하거나 숨깁니다.")
    @ApiResponse(responseCode = "200", description = "변경 성공")
    @ApiResponse(responseCode = "400", description = "요청 값 오류")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "403", description = "운영자 권한 필요")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 업체")
    @PatchMapping("/{storeId}/visibility")
    public StoreVisibilityResponse changeVisibility(
            @Parameter(description = "업체 ID", example = "100") @PathVariable Long storeId,
            @Valid @RequestBody StoreVisibilityRequest request) {
        return storeService.changeVisibility(storeId, request);
    }

    @Operation(
            summary = "업체 조건 재검토 요청 생성",
            description = "현재 정책 버전을 기준으로 RECHECK 요청을 만듭니다. 이후 입장 조건 심사 처리에서 승인할 때 적용할 정책(approvedPolicy)을 함께 보내야 합니다.")
    @ApiResponse(responseCode = "201", description = "생성 성공")
    @ApiResponse(responseCode = "400", description = "요청 값 오류")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "403", description = "운영자 권한 필요")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 업체")
    @PostMapping("/{storeId}/recheck")
    @ResponseStatus(HttpStatus.CREATED)
    public StoreRecheckResponse recheck(
            Authentication authentication,
            @Parameter(description = "업체 ID", example = "100") @PathVariable Long storeId,
            @Valid @RequestBody StoreRecheckRequest request) {
        return storeService.recheck(AdminControllerSupport.currentUserId(authentication), storeId, request);
    }
}
