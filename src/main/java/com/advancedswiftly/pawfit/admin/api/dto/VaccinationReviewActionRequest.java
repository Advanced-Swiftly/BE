package com.advancedswiftly.pawfit.admin.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "예방접종 검토 처리 요청")
public record VaccinationReviewActionRequest(
        @Schema(
                description = "VERIFY(인증 완료), NEEDS_MORE(추가 정보 요청), REQUEST_REUPLOAD(재업로드 요청)",
                example = "VERIFY",
                allowableValues = {"VERIFY", "NEEDS_MORE", "REQUEST_REUPLOAD"})
        @NotNull(message = "action은 필수입니다.")
        VaccinationReviewAction action,

        @Schema(description = "추가 정보·재업로드 요청 사유. NEEDS_MORE, REQUEST_REUPLOAD에서는 필수", nullable = true)
        @Size(max = 500, message = "사유는 500자 이하여야 합니다.")
        String comment,

        @Schema(description = "운영자가 확정한 접종 항목. VERIFY에서 전달하면 기존 운영자 확정 항목을 교체", nullable = true)
        @Size(max = 30, message = "접종 항목은 30개 이하여야 합니다.")
        List<@NotNull(message = "접종 항목은 비어 있을 수 없습니다.") @Valid Item> items) {

    @Schema(description = "운영자가 확정한 접종 항목")
    public record Item(
            @Schema(description = "백신 코드", example = "RABIES")
            @NotBlank(message = "백신 코드는 필수입니다.")
            @Size(max = 80, message = "백신 코드는 80자 이하여야 합니다.")
            String vaccineCode,

            @Schema(description = "접종일 (yyyy-MM-dd)", example = "2026-05-10")
            @NotNull(message = "접종일은 필수입니다.")
            LocalDate vaccinatedAt) {
    }
}
