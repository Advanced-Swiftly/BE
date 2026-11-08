package com.advancedswiftly.pawfit.auth.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.advancedswiftly.pawfit.auth.application.PhoneVerificationSender;
import com.jayway.jsonpath.JsonPath;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
@Import(AuthControllerIntegrationTest.SenderTestConfiguration.class)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "pawfit.auth.verification-resend-cooldown=0s"
})
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CapturingPhoneVerificationSender sender;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("delete from user_term_agreement");
        jdbcTemplate.update("delete from refresh_token");
        jdbcTemplate.update("delete from phone_verification");
        jdbcTemplate.update("delete from app_user");
        jdbcTemplate.update("delete from term");
        jdbcTemplate.update("delete from region");
        jdbcTemplate.update("delete from breed");

        jdbcTemplate.update("""
                insert into term (role, title, required, version, active)
                values ('GUARDIAN', '서비스 이용약관', true, '1.0', true)
                """);
        jdbcTemplate.update("""
                insert into term (role, title, required, version, active)
                values ('GUARDIAN', '마케팅 정보 수신 동의', false, '1.0', true)
                """);
        jdbcTemplate.update("insert into region (name, active) values ('서울특별시', true)");
        jdbcTemplate.update("insert into breed (name, active) values ('말티즈', true)");
        sender.clear();
    }

    @Test
    void guardianSignupLoginRefreshAndLogoutFollowSpecification() throws Exception {
        MvcResult sendResult = mockMvc.perform(post("/api/v1/auth/phone-verifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phoneNumber":"010-1234-5678","purpose":"SIGN_UP"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationId").isNumber())
                .andExpect(jsonPath("$.expiresInSeconds").value(180))
                .andReturn();
        Number verificationId = JsonPath.read(
                sendResult.getResponse().getContentAsString(), "$.verificationId");
        assertThat(sender.phoneNumber()).isEqualTo("01012345678");

        MvcResult confirmResult = mockMvc.perform(post("/api/v1/auth/phone-verifications/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"verificationId":%d,"code":"%s"}
                                """.formatted(verificationId.longValue(), sender.code())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(true))
                .andExpect(jsonPath("$.verificationToken").isString())
                .andReturn();
        String verificationToken = JsonPath.read(
                confirmResult.getResponse().getContentAsString(), "$.verificationToken");
        Long requiredTermId = jdbcTemplate.queryForObject(
                "select term_id from term where required = true", Long.class);

        mockMvc.perform(post("/api/v1/auth/signup/guardian")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phoneVerificationToken":"%s",
                                  "name":"김보호",
                                  "password":"password123!",
                                  "termAgreements":[{"termId":%d,"agreed":true}]
                                }
                                """.formatted(verificationToken, requiredTermId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("GUARDIAN"))
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.refreshToken").isString());

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phoneNumber":"01012345678",
                                  "password":"password123!",
                                  "deviceToken":"apns-device-token"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.name").value("김보호"))
                .andExpect(jsonPath("$.user.role").value("GUARDIAN"))
                .andReturn();
        String loginRefreshToken = JsonPath.read(
                loginResult.getResponse().getContentAsString(), "$.refreshToken");

        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/token/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(loginRefreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.refreshToken").isString())
                .andReturn();
        String rotatedAccessToken = JsonPath.read(
                refreshResult.getResponse().getContentAsString(), "$.accessToken");
        String rotatedRefreshToken = JsonPath.read(
                refreshResult.getResponse().getContentAsString(), "$.refreshToken");
        assertThat(rotatedRefreshToken).isNotEqualTo(loginRefreshToken);

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + rotatedAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s","deviceToken":"apns-device-token"}
                                """.formatted(rotatedRefreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(post("/api/v1/auth/token/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(rotatedRefreshToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void signupRejectsMissingRequiredTermAgreement() throws Exception {
        String verificationToken = issueSignupVerificationToken("01022223333");

        mockMvc.perform(post("/api/v1/auth/signup/guardian")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phoneVerificationToken":"%s",
                                  "name":"김보호",
                                  "password":"password123!",
                                  "termAgreements":[{"termId":999999,"agreed":true}]
                                }
                                """.formatted(verificationToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));
    }

    @Test
    void commonReferenceEndpointsReturnSpecificationShape() throws Exception {
        mockMvc.perform(get("/api/v1/terms").param("role", "GUARDIAN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.terms[0].title").value("서비스 이용약관"))
                .andExpect(jsonPath("$.terms[0].required").value(true));

        mockMvc.perform(get("/api/v1/regions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.regions[0].name").value("서울특별시"));

        mockMvc.perform(get("/api/v1/breeds").param("keyword", "말티"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.breeds[0].name").value("말티즈"));
    }

    @Test
    void invalidLoginAndAnonymousLogoutAreRejected() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phoneNumber":"01012345678","password":"wrong-password"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"any-token"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    private String issueSignupVerificationToken(String phoneNumber) throws Exception {
        MvcResult sendResult = mockMvc.perform(post("/api/v1/auth/phone-verifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phoneNumber":"%s","purpose":"SIGN_UP"}
                                """.formatted(phoneNumber)))
                .andReturn();
        Number verificationId = JsonPath.read(
                sendResult.getResponse().getContentAsString(), "$.verificationId");
        MvcResult confirmResult = mockMvc.perform(post("/api/v1/auth/phone-verifications/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"verificationId":%d,"code":"%s"}
                                """.formatted(verificationId.longValue(), sender.code())))
                .andReturn();
        return JsonPath.read(
                confirmResult.getResponse().getContentAsString(), "$.verificationToken");
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class SenderTestConfiguration {

        @Bean
        @Primary
        CapturingPhoneVerificationSender capturingPhoneVerificationSender() {
            return new CapturingPhoneVerificationSender();
        }
    }

    static class CapturingPhoneVerificationSender implements PhoneVerificationSender {

        private final AtomicReference<String> phoneNumber = new AtomicReference<>();
        private final AtomicReference<String> code = new AtomicReference<>();

        @Override
        public void send(String phoneNumber, String code) {
            this.phoneNumber.set(phoneNumber);
            this.code.set(code);
        }

        String phoneNumber() {
            return phoneNumber.get();
        }

        String code() {
            return code.get();
        }

        void clear() {
            phoneNumber.set(null);
            code.set(null);
        }
    }
}
