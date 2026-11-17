package com.advancedswiftly.pawfit.member;

import com.advancedswiftly.pawfit.auth.domain.UserRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.AssertTrue;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users/me")
class MemberController {
    private final MemberService service;

    MemberController(MemberService service) {
        this.service = service;
    }

    @GetMapping
    Profile profile(Authentication auth) {
        return service.profile(userId(auth));
    }

    @PatchMapping
    UpdatedProfile update(Authentication auth, @Valid @RequestBody ProfileUpdate request) {
        return service.updateProfile(userId(auth), request);
    }

    @GetMapping("/notification-settings")
    Notifications notifications(Authentication auth) {
        return service.notifications(userId(auth));
    }

    @PatchMapping("/notification-settings")
    Notifications updateNotifications(Authentication auth, @RequestBody Notifications request) {
        return service.updateNotifications(userId(auth), request);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void withdraw(Authentication auth, @Valid @RequestBody Withdrawal request) {
        service.withdraw(userId(auth), request);
    }

    @Operation(summary = "비밀번호 변경", description = "현재 비밀번호를 확인하고 모든 refresh token을 폐기합니다. 다시 로그인해야 합니다.")
    @PatchMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void changePassword(Authentication auth, @Valid @RequestBody PasswordChange request) {
        service.changePassword(userId(auth), request);
    }

    private long userId(Authentication auth) {
        return Long.parseLong(auth.getName());
    }

    record Profile(Long userId, UserRole role, String name, String phoneNumber, String loginId) {}
    record UpdatedProfile(Long userId, String name, String phoneNumber) {}
    record ProfileUpdate(@Size(max = 50) @Pattern(regexp = "(?s).*\\S.*") String name,
                         @Size(min = 1, max = 200) String phoneVerificationToken) {}
    record Notifications(Boolean event, Boolean inquiry, Boolean reviewResult, Boolean marketing) {}
    record Withdrawal(@NotBlank @Size(max = 200) String password, @Size(max = 500) String reason) {}

    record PasswordChange(
            @Schema(accessMode = Schema.AccessMode.WRITE_ONLY)
            @NotBlank @Size(max = 200) String currentPassword,
            @Schema(description = "8~72자, 영문·특수문자 포함, UTF-8 72 byte 이하", accessMode = Schema.AccessMode.WRITE_ONLY)
            @NotBlank @Size(min = 8, max = 72)
            @Pattern(regexp = "(?s)^(?=.*[a-zA-Z])(?=.*[^a-zA-Z0-9\\s]).*$")
            String newPassword) {

        @AssertTrue(message = "새 비밀번호는 UTF-8 기준 72 byte 이하여야 합니다.")
        public boolean isPasswordByteLengthValid() {
            return newPassword == null || newPassword.getBytes(StandardCharsets.UTF_8).length <= 72;
        }
    }
}
