package com.advancedswiftly.pawfit.business.api;

import com.advancedswiftly.pawfit.business.api.dto.EventEligibilityResponse;
import com.advancedswiftly.pawfit.business.application.EventEligibilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "업체 신청", description = "이벤트 참여 자격 확인")
@RestController
@RequestMapping("/api/v1/business/events")
@PreAuthorize("hasRole('BUSINESS')")
public class EventEligibilityController {

    private final EventEligibilityService service;

    public EventEligibilityController(EventEligibilityService service) {
        this.service = service;
    }

    @Operation(summary = "이벤트 참여조건 확인", description = "지역, 사업자 승인, 유효한 QR, 최신 적용 정책의 확인일(30일 이내)을 검사합니다.")
    @ApiResponse(responseCode = "200", description = "조건 충족·미충족 결과")
    @ApiResponse(responseCode = "400", description = "요청 값 오류")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "403", description = "관리 업체가 아님")
    @ApiResponse(responseCode = "404", description = "이벤트 또는 업체 없음")
    @GetMapping("/{eventId}/eligibility")
    public EventEligibilityResponse get(Authentication authentication,
            @Positive @PathVariable Long eventId, @Positive @RequestParam Long placeId) {
        return service.get(BusinessControllerSupport.currentUserId(authentication), eventId, placeId);
    }
}
