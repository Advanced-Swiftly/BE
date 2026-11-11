package com.advancedswiftly.pawfit.admin.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "예방접종 검토 상세")
public record VaccinationReviewResponse(
        @Schema(description = "기록 ID", example = "20") Long recordId,
        @Schema(description = "반려견 ID", example = "5") Long dogId,
        @Schema(description = "검토 상태", example = "PENDING_REVIEW") String status,
        @Schema(description = "증명서 문서") List<Document> documents,
        @Schema(description = "접종 항목 (AI 추출·보호자 입력·운영자 수정)") List<Item> items) {

    @Schema(description = "증명서 문서")
    public record Document(
            @Schema(description = "문서 ID", example = "31") Long documentId,
            @Schema(description = "저장소 내 파일 경로") String objectPath) {
    }

    @Schema(description = "접종 항목")
    public record Item(
            @Schema(description = "항목 ID", example = "1") Long itemId,
            @Schema(description = "출처", example = "AI", allowableValues = {"AI", "GUARDIAN", "OPERATOR"}) String source,
            @Schema(description = "백신 코드", example = "RABIES") String vaccineCode,
            @Schema(description = "접종일", example = "2026-05-10") LocalDate vaccinatedAt,
            @Schema(description = "AI 신뢰도(0~1). AI 추출 항목에만 있음", example = "0.97", nullable = true) Double confidence) {
    }
}
