package com.advancedswiftly.pawfit.auth.api;

import com.advancedswiftly.pawfit.auth.api.dto.AuthDtos.LoginRequest;
import com.advancedswiftly.pawfit.auth.api.dto.AuthDtos.LoginResponse;
import com.advancedswiftly.pawfit.auth.api.dto.AuthDtos.LogoutRequest;
import com.advancedswiftly.pawfit.auth.api.dto.AuthDtos.LogoutResponse;
import com.advancedswiftly.pawfit.auth.api.dto.AuthDtos.PhoneVerificationConfirmRequest;
import com.advancedswiftly.pawfit.auth.api.dto.AuthDtos.PhoneVerificationConfirmResponse;
import com.advancedswiftly.pawfit.auth.api.dto.AuthDtos.PhoneVerificationCreateRequest;
import com.advancedswiftly.pawfit.auth.api.dto.AuthDtos.PhoneVerificationCreateResponse;
import com.advancedswiftly.pawfit.auth.api.dto.AuthDtos.RefreshRequest;
import com.advancedswiftly.pawfit.auth.api.dto.AuthDtos.RefreshResponse;
import com.advancedswiftly.pawfit.auth.api.dto.AuthDtos.SignupRequest;
import com.advancedswiftly.pawfit.auth.api.dto.AuthDtos.SignupResponse;
import com.advancedswiftly.pawfit.auth.application.AuthService;
import com.advancedswiftly.pawfit.auth.application.PhoneVerificationService;
import com.advancedswiftly.pawfit.auth.domain.UserRole;
import com.advancedswiftly.pawfit.global.error.BusinessException;
import com.advancedswiftly.pawfit.global.error.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "공통·인증", description = "회원가입, 로그인, token, 휴대전화 인증")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final PhoneVerificationService phoneVerificationService;

    public AuthController(
            AuthService authService, PhoneVerificationService phoneVerificationService) {
        this.authService = authService;
        this.phoneVerificationService = phoneVerificationService;
    }

    @Operation(summary = "보호자 회원가입")
    @PostMapping("/signup/guardian")
    public ResponseEntity<SignupResponse> signupGuardian(
            @Valid @RequestBody SignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.signup(request, UserRole.GUARDIAN));
    }

    @Operation(summary = "업체관리자 회원가입")
    @PostMapping("/signup/business")
    public ResponseEntity<SignupResponse> signupBusiness(
            @Valid @RequestBody SignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.signup(request, UserRole.BUSINESS));
    }

    @Operation(summary = "로그인")
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @Operation(summary = "액세스 token 재발급")
    @PostMapping("/token/refresh")
    public RefreshResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @Operation(summary = "로그아웃")
    @PostMapping("/logout")
    public LogoutResponse logout(
            Authentication authentication, @Valid @RequestBody LogoutRequest request) {
        authService.logout(currentUserId(authentication), request);
        return new LogoutResponse(true);
    }

    @Operation(summary = "휴대전화 인증번호 발송")
    @PostMapping("/phone-verifications")
    public PhoneVerificationCreateResponse sendVerificationCode(
            @Valid @RequestBody PhoneVerificationCreateRequest request) {
        return phoneVerificationService.send(request.phoneNumber(), request.purpose());
    }

    @Operation(summary = "휴대전화 인증번호 확인")
    @PostMapping("/phone-verifications/confirm")
    public PhoneVerificationConfirmResponse confirmVerificationCode(
            @Valid @RequestBody PhoneVerificationConfirmRequest request) {
        return phoneVerificationService.confirm(request.verificationId(), request.code());
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
