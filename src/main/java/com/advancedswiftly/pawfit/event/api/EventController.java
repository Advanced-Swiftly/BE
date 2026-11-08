package com.advancedswiftly.pawfit.event.api;

import com.advancedswiftly.pawfit.event.api.dto.EventDtos.*;
import com.advancedswiftly.pawfit.event.application.EventService;
import com.advancedswiftly.pawfit.global.error.BusinessException;
import com.advancedswiftly.pawfit.global.error.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "방문·이벤트", description = "QR 방문, 스탬프 이벤트와 추첨 응모")
@Validated
@RestController
@RequestMapping("/api/v1")
public class EventController {
    private final EventService eventService;
    public EventController(EventService eventService) { this.eventService = eventService; }

    @Operation(summary = "현재 이벤트 조회")
    @GetMapping("/events/current")
    public CurrentEventResponse current(Authentication auth, @RequestParam(required = false) @Positive Long regionId) {
        return eventService.findCurrent(userId(auth), regionId);
    }

    @Operation(summary = "이벤트 상세 조회")
    @GetMapping("/events/{eventId}")
    public EventDetailResponse detail(Authentication auth, @PathVariable @Positive Long eventId) {
        return eventService.findDetail(userId(auth), eventId);
    }

    @Operation(summary = "이벤트 참여 업체 목록 조회")
    @GetMapping("/events/{eventId}/stores")
    public EventStoreListResponse stores(
            Authentication auth, @PathVariable @Positive Long eventId,
            @RequestParam(required = false) @Positive Long regionId,
            @RequestParam(required = false) @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
            @RequestParam(required = false) @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude) {
        return eventService.findStores(userId(auth), eventId, regionId, latitude, longitude);
    }

    @Operation(summary = "업체 QR 방문 인증")
    @PostMapping("/visits/qr")
    public ResponseEntity<QrVisitResponse> visit(
            Authentication auth, @Valid @RequestBody QrVisitRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.authenticateVisit(userId(auth), request));
    }

    @Operation(summary = "내 방문 기록 조회")
    @GetMapping("/visits")
    public VisitListResponse visits(
            Authentication auth,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") @PositiveOrZero int page) {
        return eventService.findVisits(userId(auth), from, to, page);
    }

    @Operation(summary = "이벤트 스탬프 현황 조회")
    @GetMapping("/events/{eventId}/stamps")
    public StampStatusResponse stamps(Authentication auth, @PathVariable @Positive Long eventId) {
        return eventService.findStamps(userId(auth), eventId);
    }

    @Operation(summary = "이벤트 추첨 응모")
    @PostMapping("/events/{eventId}/draw-entries")
    public ResponseEntity<DrawEntryCreateResponse> enter(
            Authentication auth, @PathVariable @Positive Long eventId,
            @Valid @RequestBody DrawEntryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.enterDraw(userId(auth), eventId, request));
    }

    @Operation(summary = "내 추첨 응모·결과 조회")
    @GetMapping("/events/{eventId}/draw-entry")
    public DrawEntryResponse drawEntry(Authentication auth, @PathVariable @Positive Long eventId) {
        return eventService.findDrawEntry(userId(auth), eventId);
    }

    private Long userId(Authentication authentication) {
        if (authentication == null) throw new BusinessException(ErrorCode.UNAUTHORIZED);
        try { return Long.valueOf(authentication.getName()); }
        catch (NumberFormatException exception) { throw new BusinessException(ErrorCode.UNAUTHORIZED); }
    }
}
