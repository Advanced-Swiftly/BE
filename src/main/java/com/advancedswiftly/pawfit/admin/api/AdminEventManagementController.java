package com.advancedswiftly.pawfit.admin.api;

import com.advancedswiftly.pawfit.admin.api.dto.AdminEventListResponse;
import com.advancedswiftly.pawfit.admin.api.dto.DrawResultResponse;
import com.advancedswiftly.pawfit.admin.api.dto.EventCreateRequest;
import com.advancedswiftly.pawfit.admin.api.dto.EventStatusResponse;
import com.advancedswiftly.pawfit.admin.api.dto.EventUpdateRequest;
import com.advancedswiftly.pawfit.admin.application.AdminEventManagementService;
import com.advancedswiftly.pawfit.event.domain.EventStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "운영자 심사", description = "운영자의 이벤트 관리와 추첨 결과 조회")
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('OPERATOR')")
public class AdminEventManagementController {

    private final AdminEventManagementService eventService;

    public AdminEventManagementController(AdminEventManagementService eventService) {
        this.eventService = eventService;
    }

    @Operation(summary = "운영자 이벤트 목록 조회", description = "연도·분기·상태로 거르고 최신순으로 20건씩 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(responseCode = "400", description = "필터 또는 page 값 오류")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "403", description = "운영자 권한 필요")
    @GetMapping("/events")
    public AdminEventListResponse listEvents(
            @Parameter(description = "연도", example = "2026") @RequestParam(required = false) String year,
            @Parameter(description = "분기 (1~4)", example = "4") @RequestParam(required = false) String quarter,
            @Parameter(
                    description = "이벤트 상태",
                    schema = @Schema(type = "string", allowableValues = {"DRAFT", "OPEN", "CLOSED", "DRAWN"}))
            @RequestParam(required = false) String status,
            @Parameter(description = "페이지 번호 (1부터)", example = "1") @RequestParam(required = false) String page) {
        return eventService.list(
                AdminControllerSupport.optionalInt("year", year),
                AdminControllerSupport.optionalInt("quarter", quarter),
                AdminControllerSupport.optionalEnum(EventStatus.class, "status", status),
                AdminControllerSupport.parsePage(page));
    }

    @Operation(summary = "이벤트 생성", description = "초안(DRAFT)으로 만듭니다. 같은 연도·분기 이벤트가 있으면 DUPLICATE_EVENT_PERIOD(409)입니다.")
    @ApiResponse(responseCode = "201", description = "생성 성공")
    @ApiResponse(responseCode = "400", description = "요청 값 오류")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "403", description = "운영자 권한 필요")
    @ApiResponse(responseCode = "409", description = "DUPLICATE_EVENT_PERIOD")
    @PostMapping("/events")
    @ResponseStatus(HttpStatus.CREATED)
    public EventStatusResponse createEvent(@Valid @RequestBody EventCreateRequest request) {
        return eventService.create(request);
    }

    @Operation(
            summary = "이벤트 수정·공개",
            description = "초안만 수정할 수 있습니다. 전달한 항목만 바뀌고 regionIds·prizes는 전체가 교체됩니다. "
                    + "status를 OPEN으로 보내면 공개하며 지역과 경품이 있어야 합니다. 초안이 아니면 EVENT_NOT_EDITABLE(409)입니다.")
    @ApiResponse(responseCode = "200", description = "수정 성공")
    @ApiResponse(responseCode = "400", description = "요청 값 오류")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "403", description = "운영자 권한 필요")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 이벤트")
    @ApiResponse(responseCode = "409", description = "EVENT_NOT_EDITABLE")
    @PatchMapping("/events/{eventId}")
    public EventStatusResponse updateEvent(
            @Parameter(description = "이벤트 ID", example = "801") @PathVariable Long eventId,
            @Valid @RequestBody EventUpdateRequest request) {
        return eventService.update(eventId, request);
    }

    @Operation(summary = "이벤트 삭제", description = "초안이고 참여 업체·응모가 없을 때만 삭제합니다. 아니면 EVENT_NOT_DELETABLE(409)입니다.")
    @ApiResponse(responseCode = "204", description = "삭제 성공")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "403", description = "운영자 권한 필요")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 이벤트")
    @ApiResponse(responseCode = "409", description = "EVENT_NOT_DELETABLE")
    @DeleteMapping("/events/{eventId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEvent(@Parameter(description = "이벤트 ID", example = "801") @PathVariable Long eventId) {
        eventService.delete(eventId);
    }

    @Operation(summary = "추첨 결과 조회", description = "당첨자와 당첨 취소자를 경품 순위순으로 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "403", description = "운영자 권한 필요")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 이벤트")
    @GetMapping("/events/{eventId}/draw-result")
    public DrawResultResponse drawResult(
            @Parameter(description = "이벤트 ID", example = "801") @PathVariable Long eventId) {
        return eventService.drawResult(eventId);
    }
}
