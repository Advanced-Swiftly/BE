package com.advancedswiftly.pawfit.auth.api.dto;

import com.advancedswiftly.pawfit.auth.domain.PhoneVerificationPurpose;
import com.advancedswiftly.pawfit.auth.domain.UserRole;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class AuthDtos {

    private AuthDtos() {}

    public record PhoneVerificationCreateRequest(
            @NotBlank
                    @Pattern(
                            regexp = "^(?:\\+82[- ]?|0)10[- ]?\\d{4}[- ]?\\d{4}$",
                            message = "올바른 휴대전화 번호 형식이어야 합니다.")
                    String phoneNumber,
            @NotNull PhoneVerificationPurpose purpose) {}

    public record PhoneVerificationCreateResponse(Long verificationId, long expiresInSeconds) {}

    public record PhoneVerificationConfirmRequest(
            @NotNull @Positive Long verificationId,
            @NotBlank @Pattern(regexp = "^\\d{6}$", message = "인증번호는 6자리 숫자여야 합니다.") String code) {}

    public record PhoneVerificationConfirmResponse(boolean verified, String verificationToken) {}

    public record TermAgreementRequest(@NotNull @Positive Long termId, boolean agreed) {}

    public record SignupRequest(
            @NotBlank String phoneVerificationToken,
            @NotBlank @Size(max = 50) String name,
            @NotBlank @Size(min = 8, max = 72) String password,
            @NotEmpty List<@Valid TermAgreementRequest> termAgreements,
            @Email @Size(min = 3, max = 254) String loginId) {}

    public record SignupResponse(
            Long userId, UserRole role, String accessToken, String refreshToken) {}

    public record LoginRequest(
            @Pattern(
                            regexp = "^(?:\\+82[- ]?|0)10[- ]?\\d{4}[- ]?\\d{4}$",
                            message = "올바른 휴대전화 번호 형식이어야 합니다.")
                    String phoneNumber,
            @NotBlank String password,
            @Size(max = 255) String deviceToken,
            @Email @Size(min = 3, max = 254) String loginId) {

        public LoginRequest {
            phoneNumber = phoneNumber == null || phoneNumber.isBlank() ? null : phoneNumber.strip();
            loginId = loginId == null || loginId.isBlank() ? null : loginId.strip();
        }

        @AssertTrue(message = "phoneNumber 또는 loginId 중 하나만 입력해야 합니다.")
        public boolean isIdentifierValid() {
            return (phoneNumber != null && !phoneNumber.isBlank())
                    ^ (loginId != null && !loginId.isBlank());
        }
    }

    public record UserSummary(Long userId, UserRole role, String name) {}

    public record LoginResponse(UserSummary user, String accessToken, String refreshToken) {}

    public record RefreshRequest(@NotBlank String refreshToken) {}

    public record RefreshResponse(String accessToken, String refreshToken) {}

    public record LogoutRequest(
            @NotBlank String refreshToken, @Size(max = 255) String deviceToken) {}

    public record LogoutResponse(boolean success) {}
}
