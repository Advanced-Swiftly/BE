package com.advancedswiftly.pawfit.inquiry.api;

import com.advancedswiftly.pawfit.global.error.BusinessException;
import com.advancedswiftly.pawfit.global.error.ErrorCode;
import com.advancedswiftly.pawfit.inquiry.api.dto.InquiryDtos.InquiryCreateRequest;
import com.advancedswiftly.pawfit.inquiry.api.dto.InquiryDtos.InquiryCreateResponse;
import com.advancedswiftly.pawfit.inquiry.api.dto.InquiryDtos.InquiryDetailResponse;
import com.advancedswiftly.pawfit.inquiry.api.dto.InquiryDtos.InquiryListResponse;
import com.advancedswiftly.pawfit.inquiry.application.InquiryService;
import com.advancedswiftly.pawfit.inquiry.domain.InquiryStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "문의", description = "사용자의 1:1 문의 등록·조회")
@Validated
@RestController
@RequestMapping("/api/v1/inquiries")
public class InquiryController {

    private final InquiryService inquiryService;

    public InquiryController(InquiryService inquiryService) {
        this.inquiryService = inquiryService;
    }

    @Operation(summary = "1:1 문의 등록")
    @PostMapping
    public ResponseEntity<InquiryCreateResponse> create(
            Authentication authentication,
            @Valid @RequestBody InquiryCreateRequest request) {
        InquiryCreateResponse response = inquiryService.create(
                currentUserId(authentication), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "내 문의 목록 조회")
    @GetMapping
    public InquiryListResponse findMine(
            Authentication authentication,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") @PositiveOrZero int page) {
        return inquiryService.findMine(
                currentUserId(authentication), parseStatus(status), page);
    }

    @Operation(summary = "문의 상세 조회")
    @GetMapping("/{inquiryId}")
    public InquiryDetailResponse findMine(
            Authentication authentication,
            @PathVariable @Positive Long inquiryId) {
        return inquiryService.findMine(currentUserId(authentication), inquiryId);
    }

    private InquiryStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return InquiryStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }

    private Long currentUserId(Authentication authentication) {
        if (authentication == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        try {
            return Long.valueOf(authentication.getName());
        } catch (NumberFormatException exception) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }
}
