package com.advancedswiftly.pawfit.member;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.advancedswiftly.pawfit.auth.application.TokenService;
import com.advancedswiftly.pawfit.auth.domain.PhoneVerification;
import com.advancedswiftly.pawfit.auth.domain.PhoneVerificationPurpose;
import com.advancedswiftly.pawfit.auth.domain.User;
import com.advancedswiftly.pawfit.auth.domain.UserRole;
import com.advancedswiftly.pawfit.auth.infrastructure.PhoneVerificationRepository;
import com.advancedswiftly.pawfit.auth.infrastructure.UserRepository;
import com.advancedswiftly.pawfit.auth.application.PhoneVerificationService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:member;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class MemberIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwords;
    @Autowired TokenService tokens;
    @Autowired PhoneVerificationRepository verifications;
    @Autowired PhoneVerificationService verificationService;

    @Test
    void managesOwnProfilePreferencesAndInvalidatesTokensOnWithdrawal() throws Exception {
        var owner = users.saveAndFlush(User.register(
                "01012345678", passwords.encode("password123!"), "보호자", UserRole.GUARDIAN));
        users.saveAndFlush(User.register(
                "01011112222", passwords.encode("password123!"), "다른 회원", UserRole.BUSINESS));
        var issued = tokens.issue(owner, null);
        String auth = "Bearer " + issued.accessToken();
        String me = "/api/v1/users/me";
        String notifications = me + "/notification-settings";
        mvc.perform(get(me)).andExpect(status().isUnauthorized());
        mvc.perform(get(me).header("Authorization", auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("GUARDIAN"));
        String duplicate = verifiedToken("01011112222", PhoneVerificationPurpose.CHANGE_PHONE);
        mvc.perform(patch(me).header("Authorization", auth).contentType("application/json")
                        .content("{\"phoneVerificationToken\":\"%s\"}".formatted(duplicate)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("DUPLICATE_PHONE_NUMBER"));
        String signup = verifiedToken("01033334444", PhoneVerificationPurpose.SIGN_UP);
        mvc.perform(patch(me).header("Authorization", auth).contentType("application/json")
                        .content("{\"phoneVerificationToken\":\"%s\"}".formatted(signup)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_VERIFICATION_TOKEN"));
        String change = verifiedToken("01033334444", PhoneVerificationPurpose.CHANGE_PHONE);
        String update = "{\"name\":\"콩이 보호자\",\"phoneVerificationToken\":\"%s\"}".formatted(change);
        mvc.perform(patch(me).header("Authorization", auth).contentType("application/json").content(update))
                .andExpect(status().isOk()).andExpect(jsonPath("$.phoneNumber").value("01033334444"));
        mvc.perform(patch(me).header("Authorization", auth).contentType("application/json").content(update))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_VERIFICATION_TOKEN"));
        mvc.perform(get(notifications).header("Authorization", auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.marketing").value(false));
        mvc.perform(patch(notifications).header("Authorization", auth).contentType("application/json")
                        .content("{\"event\":false}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.event").value(false))
                .andExpect(jsonPath("$.inquiry").value(true));
        mvc.perform(delete(me).header("Authorization", auth).contentType("application/json")
                        .content("{\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("INVALID_PASSWORD"));
        mvc.perform(delete(me).header("Authorization", auth).contentType("application/json")
                        .content("{\"password\":\"password123!\"}"))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        mvc.perform(get(me).header("Authorization", auth)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/token/refresh").contentType("application/json")
                        .content("{\"refreshToken\":\"%s\"}".formatted(issued.refreshToken())))
                .andExpect(status().isUnauthorized());
    }

    private String verifiedToken(String phone, PhoneVerificationPurpose purpose) {
        var verification = verifications.saveAndFlush(PhoneVerification.start(
                phone, purpose, passwords.encode("123456"), Instant.now().plusSeconds(300)));
        return verificationService.confirm(verification.getId(), "123456").verificationToken();
    }
}
