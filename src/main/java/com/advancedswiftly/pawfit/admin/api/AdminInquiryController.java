package com.advancedswiftly.pawfit.admin.api;

import com.advancedswiftly.pawfit.admin.api.dto.AdminInquiryDetailResponse;
import com.advancedswiftly.pawfit.admin.api.dto.InquiryAnswerRequest;
import com.advancedswiftly.pawfit.admin.api.dto.InquiryAnswerResponse;
import com.advancedswiftly.pawfit.admin.api.dto.InquiryListResponse;
import com.advancedswiftly.pawfit.admin.application.AdminInquiryService;
import com.advancedswiftly.pawfit.inquiry.domain.InquiryCategory;
import com.advancedswiftly.pawfit.inquiry.domain.InquiryStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "운영자 심사", description = "운영자의 사용자 문의 조회·답변")
@RestController
@RequestMapping("/api/v1/admin/inquiries")
@PreAuthorize("hasRole('OPERATOR')")
public class AdminInquiryController {

    private final AdminInquiryService inquiryService;

    public AdminInquiryController(AdminInquiryService inquiryService) {
        this.inquiryService = inquiryService;
    }

    @Operation(summary = "문의 목록 조회", description = "답변 상태·카테고리별 사용자 문의를 최신순으로 20건씩 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(responseCode = "400", description = "status, category, page 값 오류")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "403", description = "운영자 권한 필요")
    @GetMapping
    public InquiryListResponse list(
            @Parameter(
                    description = "답변 상태",
                    schema = @Schema(type = "string", allowableValues = {"WAITING", "ANSWERED"}))
            @RequestParam(required = false) String status,
            @Parameter(
                    description = "문의 카테고리",
                    schema = @Schema(type = "string", allowableValues = {"ACCOUNT", "PLACE", "EVENT", "ETC"}))
            @RequestParam(required = false) String category,
            @Parameter(description = "페이지 번호 (1부터)", example = "1") @RequestParam(required = false) String page) {
        return inquiryService.list(
                AdminControllerSupport.optionalEnum(InquiryStatus.class, "status", status),
                AdminControllerSupport.optionalEnum(InquiryCategory.class, "category", category),
                AdminControllerSupport.parsePage(page));
    }

    @Operation(summary = "운영자 문의 상세 조회", description = "문의자 정보·본문·첨부파일·답변을 조회합니다. 답변 전이면 answer, answeredAt은 응답에서 빠집니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "403", description = "운영자 권한 필요")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 문의")
    @GetMapping("/{inquiryId}")
    public AdminInquiryDetailResponse get(
            @Parameter(description = "문의 ID", example = "701") @PathVariable Long inquiryId) {
        return inquiryService.get(inquiryId);
    }

    @Operation(summary = "문의 답변 등록", description = "대기 중인 문의에 답변을 등록합니다. 이미 답변된 문의는 INQUIRY_ALREADY_ANSWERED(409)입니다.")
    @ApiResponse(responseCode = "201", description = "등록 성공")
    @ApiResponse(responseCode = "400", description = "요청 값 오류")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "403", description = "운영자 권한 필요")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 문의")
    @ApiResponse(responseCode = "409", description = "INQUIRY_ALREADY_ANSWERED")
    @PostMapping("/{inquiryId}/answer")
    public ResponseEntity<InquiryAnswerResponse> answer(
            @Parameter(description = "문의 ID", example = "701") @PathVariable Long inquiryId,
            @Valid @RequestBody InquiryAnswerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inquiryService.answer(inquiryId, request));
    }
}
