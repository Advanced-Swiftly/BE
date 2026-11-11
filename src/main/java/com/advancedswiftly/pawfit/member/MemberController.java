package com.advancedswiftly.pawfit.member;

import com.advancedswiftly.pawfit.auth.domain.UserRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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

    private long userId(Authentication auth) {
        return Long.parseLong(auth.getName());
    }

    record Profile(Long userId, UserRole role, String name, String phoneNumber) {}
    record UpdatedProfile(Long userId, String name, String phoneNumber) {}
    record ProfileUpdate(@Size(max = 50) @Pattern(regexp = "(?s).*\\S.*") String name,
                         @Size(min = 1, max = 200) String phoneVerificationToken) {}
    record Notifications(Boolean event, Boolean inquiry, Boolean reviewResult, Boolean marketing) {}
    record Withdrawal(@NotBlank @Size(max = 200) String password, @Size(max = 500) String reason) {}
}
