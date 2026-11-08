package com.advancedswiftly.pawfit.inquiry.api.dto;

import com.advancedswiftly.pawfit.inquiry.domain.InquiryCategory;
import com.advancedswiftly.pawfit.inquiry.domain.InquiryStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

public final class InquiryDtos {

    private InquiryDtos() {
    }

    public record InquiryCreateRequest(
            @NotNull InquiryCategory category,
            @NotBlank @Size(max = 100) String title,
            @NotBlank @Size(max = 4000) String content,
            @Size(max = 5) List<
                    @NotBlank
                    @Size(max = 500)
                    @Pattern(regexp = "^https://.+", message = "첨부파일 URL은 https 형식이어야 합니다.")
                    String> attachmentUrls) {

        public InquiryCreateRequest {
            attachmentUrls = attachmentUrls == null ? List.of() : List.copyOf(attachmentUrls);
        }
    }

    public record InquiryCreateResponse(Long inquiryId, InquiryStatus status) {
    }

    public record InquiryListItem(Long inquiryId, String title, InquiryStatus status) {
    }

    public record InquiryListResponse(List<InquiryListItem> items) {
    }

    public record InquiryDetailResponse(
            Long inquiryId,
            InquiryCategory category,
            String title,
            String content,
            List<String> attachmentUrls,
            InquiryStatus status,
            String answer,
            Instant createdAt,
            Instant answeredAt) {
    }
}
