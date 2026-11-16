package com.advancedswiftly.pawfit.auth.api;

import com.advancedswiftly.pawfit.auth.application.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "업체 인증", description = "업체 계정의 이메일 아이디 확인")
@RestController
@RequestMapping("/api/v1/business/auth")
public class BusinessAuthController {

    private final AuthService authService;

    public BusinessAuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "이메일 아이디 중복 확인", description = "대소문자를 구분하지 않습니다. 사용 가능 여부는 예약을 의미하지 않습니다.")
    @PostMapping("/login-id/check")
    public LoginIdCheckResponse checkLoginId(@Valid @RequestBody LoginIdCheckRequest request) {
        return new LoginIdCheckResponse(authService.isLoginIdAvailable(request.loginId()));
    }

    public record LoginIdCheckRequest(
            @Schema(description = "가입할 이메일 아이디", example = "owner@example.com")
            @NotBlank @Email @Size(max = 254) String loginId) {
    }

    public record LoginIdCheckResponse(boolean available) {
    }
}
