package com.advancedswiftly.pawfit.admin.api;

import com.advancedswiftly.pawfit.admin.api.dto.AdminEventDetailResponse;
import com.advancedswiftly.pawfit.admin.api.dto.DrawEntryActionRequest;
import com.advancedswiftly.pawfit.admin.api.dto.DrawEntryActionResponse;
import com.advancedswiftly.pawfit.admin.api.dto.DrawEntryListResponse;
import com.advancedswiftly.pawfit.admin.application.AdminEventService;
import com.advancedswiftly.pawfit.event.domain.DrawEntryStatus;
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

@Tag(name = "운영자 심사", description = "운영자의 이벤트 조회와 추첨·당첨 배송 처리")
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('OPERATOR')")
public class AdminEventController {

    private final AdminEventService eventService;

    public AdminEventController(AdminEventService eventService) {
        this.eventService = eventService;
    }

    @Operation(summary = "운영자 이벤트 상세 조회", description = "이벤트 설정·대상 지역·경품과 참여 업체·응모 수를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "403", description = "운영자 권한 필요")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 이벤트")
    @GetMapping("/events/{eventId}")
    public AdminEventDetailResponse getEvent(
            @Parameter(description = "이벤트 ID", example = "801") @PathVariable Long eventId) {
        return eventService.getEvent(eventId);
    }

    @Operation(summary = "추첨 응모자 목록 조회", description = "이벤트 응모자와 스탬프 수·연락·배송 상태를 최신순으로 20건씩 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(responseCode = "400", description = "status 또는 page 값 오류")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "403", description = "운영자 권한 필요")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 이벤트")
    @GetMapping("/events/{eventId}/draw-entries")
    public DrawEntryListResponse listDrawEntries(
            @Parameter(description = "이벤트 ID", example = "801") @PathVariable Long eventId,
            @Parameter(
                    description = "응모 상태",
                    schema = @Schema(type = "string", allowableValues = {"ENTERED", "WON", "NOT_SELECTED", "CANCELED"}))
            @RequestParam(required = false) String status,
            @Parameter(description = "페이지 번호 (1부터)", example = "1") @RequestParam(required = false) String page) {
        return eventService.listDrawEntries(
                eventId,
                AdminControllerSupport.optionalEnum(DrawEntryStatus.class, "status", status),
                AdminControllerSupport.parsePage(page));
    }

    @Operation(
            summary = "당첨·배송 상태 변경",
            description = "당첨된 응모의 배송 단계를 진행합니다. CONTACTED(연락 완료) → ADDRESS_CONFIRMED(주소 확인) → "
                    + "SHIPPED(발송, 운송장 필수) → DELIVERED(배송 완료). CANCEL_WIN은 당첨을 취소합니다. "
                    + "당첨 상태가 아니거나 순서가 맞지 않으면 INVALID_REQUEST_STATE(400)입니다.")
    @ApiResponse(responseCode = "200", description = "처리 성공")
    @ApiResponse(responseCode = "400", description = "요청 값 오류 또는 INVALID_REQUEST_STATE")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "403", description = "운영자 권한 필요")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 응모")
    @PatchMapping("/draw-entries/{drawEntryId}")
    public DrawEntryActionResponse actOnDrawEntry(
            Authentication authentication,
            @Parameter(description = "추첨 응모 ID", example = "1001") @PathVariable Long drawEntryId,
            @Valid @RequestBody DrawEntryActionRequest request) {
        return eventService.act(AdminControllerSupport.currentUserId(authentication), drawEntryId, request);
    }
}
