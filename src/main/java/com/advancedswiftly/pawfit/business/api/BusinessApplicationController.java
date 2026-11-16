package com.advancedswiftly.pawfit.business.api;

import com.advancedswiftly.pawfit.business.api.dto.AdApplicationUpdateRequest;
import com.advancedswiftly.pawfit.business.api.dto.AdApplicationUpdateResponse;
import com.advancedswiftly.pawfit.business.application.AdApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "업체 신청", description = "프론트의 광고 기간 변경 요청")
@RestController
@RequestMapping("/api/v1/business/applications")
@PreAuthorize("hasRole('BUSINESS')")
public class BusinessApplicationController {

    private final AdApplicationService service;

    public BusinessApplicationController(AdApplicationService service) {
        this.service = service;
    }

    @Operation(summary = "광고 신청 기간 수정", description = "광고 신청 ID를 사용합니다. RECEIVED 상태에서 시작일·주 수를 변경하며 종료일은 서버가 계산합니다.")
    @ApiResponse(responseCode = "200", description = "변경 성공")
    @ApiResponse(responseCode = "400", description = "유효하지 않은 요청 또는 수정할 수 없는 상태")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "403", description = "관리 업체가 아님")
    @ApiResponse(responseCode = "404", description = "존재하지 않거나 본인의 광고 신청이 아님")
    @PatchMapping("/{applicationId}")
    public AdApplicationUpdateResponse update(Authentication authentication,
            @Positive @PathVariable Long applicationId, @Valid @RequestBody AdApplicationUpdateRequest request) {
        return service.update(BusinessControllerSupport.currentUserId(authentication), applicationId, request);
    }
}
