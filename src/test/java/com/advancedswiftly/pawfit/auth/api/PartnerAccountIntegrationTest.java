package com.advancedswiftly.pawfit.auth.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.advancedswiftly.pawfit.auth.application.PhoneVerificationService;
import com.advancedswiftly.pawfit.auth.application.TokenService;
import com.advancedswiftly.pawfit.auth.domain.PhoneVerification;
import com.advancedswiftly.pawfit.auth.domain.PhoneVerificationPurpose;
import com.advancedswiftly.pawfit.auth.domain.User;
import com.advancedswiftly.pawfit.auth.domain.UserRole;
import com.advancedswiftly.pawfit.auth.infrastructure.PhoneVerificationRepository;
import com.advancedswiftly.pawfit.auth.infrastructure.UserRepository;
import com.advancedswiftly.pawfit.business.application.BusinessStatusClient;
import com.advancedswiftly.pawfit.global.error.BusinessException;
import com.advancedswiftly.pawfit.global.error.ErrorCode;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Transactional
class PartnerAccountIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private UserRepository users;
    @Autowired private PasswordEncoder passwords;
    @Autowired private TokenService tokens;
    @Autowired private PhoneVerificationRepository verifications;
    @Autowired private PhoneVerificationService phoneVerification;
    @Autowired private JdbcTemplate jdbc;
    @MockitoBean private BusinessStatusClient businessStatus;

    private User owner() {
        User user = User.register("01081234567", passwords.encode("password123!"), "업체관리자", UserRole.BUSINESS);
        user.assignLoginId("owner@example.com");
        return users.saveAndFlush(user);
    }

    @Test
    void loginIdCheckIsPublicCaseInsensitiveAndIncludesWithdrawnAccounts() throws Exception {
        User owner = owner();
        owner.withdraw(null);
        users.flush();
        mvc.perform(post("/api/v1/business/auth/login-id/check").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginId\":\"OWNER@EXAMPLE.COM\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.available").value(false));
        mvc.perform(post("/api/v1/business/auth/login-id/check").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginId\":\"new@example.com\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.available").value(true));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"loginId\":\"\"}", "{\"loginId\":\"not-an-email\"}"})
    void rejectsInvalidLoginIds(String body) throws Exception {
        mvc.perform(post("/api/v1/business/auth/login-id/check").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_001"));
    }

    @Test
    void signupStoresNormalizedEmailAndBothEmailAndPhoneLoginWork() throws Exception {
        long termId = addBusinessTerm();
        String verificationToken = signupToken("01081234568");
        mvc.perform(post("/api/v1/auth/signup/business").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phoneVerificationToken":"%s","name":"업체관리자","password":"password123!",
                                 "loginId":"NEW@EXAMPLE.COM","termAgreements":[{"termId":%d,"agreed":true}]}
                                """.formatted(verificationToken, termId)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.role").value("BUSINESS"));
        assertThat(users.findByLoginId("new@example.com")).isPresent();
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginId\":\"NEW@EXAMPLE.COM\",\"password\":\"password123!\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.accessToken").isString());
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"01081234568\",\"password\":\"password123!\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void duplicateEmailIsRejectedAtSignupEvenAfterAvailabilityCheck() throws Exception {
        owner();
        long termId = addBusinessTerm();
        String verificationToken = signupToken("01081234568");
        mvc.perform(post("/api/v1/auth/signup/business").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phoneVerificationToken":"%s","name":"중복","password":"password123!",
                                 "loginId":"OWNER@EXAMPLE.COM","termAgreements":[{"termId":%d,"agreed":true}]}
                                """.formatted(verificationToken, termId)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("DUPLICATE_LOGIN_ID"));
    }

    @Test
    void loginRequiresExactlyOneIdentifier() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"password123!\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"01081234567\",\"loginId\":\"owner@example.com\",\"password\":\"password123!\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changesPasswordAndRevokesEveryExistingRefreshToken() throws Exception {
        User owner = owner();
        var first = tokens.issue(owner, "first-device");
        var second = tokens.issue(owner, "second-device");
        mvc.perform(patch("/api/v1/users/me/password").header("Authorization", "Bearer " + first.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"password123!\",\"newPassword\":\"newPassword456!\"}"))
                .andExpect(status().isNoContent());
        String hash = users.findById(owner.getId()).orElseThrow().getPasswordHash();
        assertThat(hash).isNotEqualTo("newPassword456!");
        assertThat(passwords.matches("newPassword456!", hash)).isTrue();
        users.flush();
        assertThat(jdbc.queryForObject("select count(*) from refresh_token where user_id = ? and revoked_at is null",
                Integer.class, owner.getId())).isZero();
        for (String refresh : new String[] {first.refreshToken(), second.refreshToken()}) {
            mvc.perform(post("/api/v1/auth/token/refresh").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"refreshToken\":\"%s\"}".formatted(refresh)))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginId\":\"owner@example.com\",\"password\":\"password123!\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginId\":\"owner@example.com\",\"password\":\"newPassword456!\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void incorrectCurrentPasswordDoesNotChangePasswordOrRevokeTokens() throws Exception {
        User owner = owner();
        var issued = tokens.issue(owner, null);
        String oldHash = owner.getPasswordHash();
        mvc.perform(patch("/api/v1/users/me/password").header("Authorization", "Bearer " + issued.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"wrong-password\",\"newPassword\":\"newPassword456!\"}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("INVALID_PASSWORD"));
        assertThat(owner.getPasswordHash()).isEqualTo(oldHash);
        assertThat(jdbc.queryForObject("select count(*) from refresh_token where user_id = ? and revoked_at is null",
                Integer.class, owner.getId())).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"short!", "noSpecial123", "12345678!", "password123!", "가가가가가가가가가가가가가가가가가가가가가가가가가가가가가가A!"})
    void rejectsInvalidOrUnchangedNewPasswords(String newPassword) throws Exception {
        User owner = owner();
        mvc.perform(patch("/api/v1/users/me/password").with(user(owner.getId().toString()).roles("BUSINESS"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"password123!\",\"newPassword\":\"%s\"}".formatted(newPassword)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void anonymousPasswordChangeIsUnauthorized() throws Exception {
        mvc.perform(patch("/api/v1/users/me/password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"password123!\",\"newPassword\":\"newPassword456!\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void businessStatusUsesNormalizedNumberAndReturnsFrontendShape() throws Exception {
        when(businessStatus.isActive("1234567890")).thenReturn(true);
        mvc.perform(post("/api/v1/business/verifications/business-status").with(user("1").roles("BUSINESS"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"businessNumber\":\"123-45-67890\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.isActive").value(true));
        verify(businessStatus).isActive("1234567890");
    }

    @Test
    void externalBusinessStatusFailureIs503() throws Exception {
        when(businessStatus.isActive("1234567890")).thenThrow(new BusinessException(ErrorCode.BUSINESS_STATUS_UNAVAILABLE));
        mvc.perform(post("/api/v1/business/verifications/business-status").with(user("1").roles("BUSINESS"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"businessNumber\":\"1234567890\"}"))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("BUSINESS_STATUS_UNAVAILABLE"));
    }

    @Test
    void businessStatusRejectsAnonymousGuardianAndMalformedNumberBeforeCallingProvider() throws Exception {
        String path = "/api/v1/business/verifications/business-status";
        mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content("{\"businessNumber\":\"1234567890\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post(path).with(user("1").roles("GUARDIAN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"businessNumber\":\"1234567890\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post(path).with(user("1").roles("BUSINESS"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"businessNumber\":\"12345\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(businessStatus);
    }

    private long addBusinessTerm() {
        jdbc.update("insert into term (role, title, required, version, active) values ('BUSINESS', '약관', false, '1.0', true)");
        return jdbc.queryForObject("select max(term_id) from term where role = 'BUSINESS'", Long.class);
    }

    private String signupToken(String phone) {
        PhoneVerification verification = verifications.saveAndFlush(PhoneVerification.start(
                phone, PhoneVerificationPurpose.SIGN_UP, passwords.encode("123456"), Instant.now().plusSeconds(180)));
        return phoneVerification.confirm(verification.getId(), "123456").verificationToken();
    }
}
