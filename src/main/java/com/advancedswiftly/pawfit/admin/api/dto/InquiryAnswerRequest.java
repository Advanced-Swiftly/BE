package com.advancedswiftly.pawfit.admin.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "문의 답변 등록 요청")
public record InquiryAnswerRequest(
        @Schema(description = "답변 내용", example = "확인 후 스탬프를 반영해 드렸습니다.")
        @NotBlank(message = "답변 내용은 필수입니다.")
        @Size(max = 4000, message = "답변은 4000자 이하여야 합니다.")
        String answer,

        @Schema(description = "답변 첨부파일 URL (최대 10개)", nullable = true)
        @Size(max = 10, message = "첨부파일은 10개 이하여야 합니다.")
        List<@NotBlank(message = "첨부파일 URL은 비어 있을 수 없습니다.")
                @Size(max = 500, message = "첨부파일 URL은 500자 이하여야 합니다.")
                @Pattern(regexp = "^https?://.+", message = "첨부파일 URL 형식이 올바르지 않습니다.") String> attachmentUrls) {
}
