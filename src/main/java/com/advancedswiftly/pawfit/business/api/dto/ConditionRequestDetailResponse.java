package com.advancedswiftly.pawfit.business.api.dto;

import com.advancedswiftly.pawfit.business.domain.ConditionRequest;
import com.advancedswiftly.pawfit.business.domain.ConditionRequestStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "입장 조건 변경 요청 상세")
public record ConditionRequestDetailResponse(
        @Schema(description = "요청 ID", example = "1301") Long requestId,
        @Schema(description = "처리 상태", example = "NEEDS_SUPPLEMENT") ConditionRequestStatus status,
        @Schema(
                        description = "심사 의견(보완 요청·반려 사유). 심사 전이면 필드가 응답에서 빠집니다",
                        example = "예방접종 기준을 명시해 주세요.",
                        nullable = true)
                String reviewComment,
        Long storeId,
        int baseVersion,
        String sourceText,
        RequestedPolicy requestedPolicy,
        java.util.List<String> evidenceUrls,
        java.time.Instant submittedAt,
        java.time.Instant reviewedAt,
        boolean canCancel,
        boolean canResubmit) {

    private static java.util.List<PolicyRuleDto> rules(ConditionRequest request, String type) {
        return request.getRequestedRules().stream()
                .filter(r -> r.getType().name().equals(type))
                .map(PolicyRuleDto::from)
                .toList();
    }

    public static ConditionRequestDetailResponse from(ConditionRequest request) {
        return from(request, java.util.List.copyOf(request.getEvidenceUrls()));
    }

    public static ConditionRequestDetailResponse from(
            ConditionRequest request, java.util.List<String> displayEvidence) {
        return new ConditionRequestDetailResponse(
                request.getId(),
                request.getStatus(),
                request.getReviewNote(),
                request.getStoreId(),
                request.getBaseVersion(),
                request.getSourceText(),
                new RequestedPolicy(
                        rules(request, "BREED"),
                        rules(request, "EQUIPMENT"),
                        rules(request, "VACCINE"),
                        request.getConditions()),
                displayEvidence,
                request.getCreatedAt(),
                request.getReviewedAt(),
                request.canCancel(),
                request.canResubmit());
    }
}
