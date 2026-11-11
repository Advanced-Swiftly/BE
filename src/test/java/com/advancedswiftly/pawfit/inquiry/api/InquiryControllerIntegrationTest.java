package com.advancedswiftly.pawfit.inquiry.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.advancedswiftly.pawfit.auth.domain.User;
import com.advancedswiftly.pawfit.auth.domain.UserRole;
import com.advancedswiftly.pawfit.auth.infrastructure.UserRepository;
import com.advancedswiftly.pawfit.inquiry.domain.InquiryStatus;
import com.advancedswiftly.pawfit.inquiry.infrastructure.InquiryRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class InquiryControllerIntegrationTest {

    private static final String CREATE_BODY = """
            {
              "category":"EVENT",
              "title":"이벤트 문의",
              "content":"이벤트 적용 조건을 확인하고 싶습니다.",
              "attachmentUrls":["https://example.com/inquiries/image.png"]
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InquiryRepository inquiryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User owner;
    private User otherUser;

    @BeforeEach
    void setUp() {
        inquiryRepository.deleteAll();
        userRepository.deleteAll();
        owner = userRepository.saveAndFlush(User.register(
                "01011112222", passwordEncoder.encode("password123!"), "문의자", UserRole.GUARDIAN));
        otherUser = userRepository.saveAndFlush(User.register(
                "01033334444", passwordEncoder.encode("password123!"), "다른 사용자", UserRole.GUARDIAN));
    }

    @Test
    void authenticatedUserCanCreateListAndReadOwnInquiry() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/v1/inquiries")
                        .with(user(owner.getId().toString()).roles("GUARDIAN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.inquiryId").isNumber())
                .andExpect(jsonPath("$.status").value("WAITING"))
                .andReturn();
        Number inquiryId = JsonPath.read(
                createResult.getResponse().getContentAsString(), "$.inquiryId");

        mockMvc.perform(get("/api/v1/inquiries")
                        .with(user(owner.getId().toString()).roles("GUARDIAN"))
                        .param("status", "WAITING")
                        .param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].inquiryId").value(inquiryId.longValue()))
                .andExpect(jsonPath("$.items[0].title").value("이벤트 문의"))
                .andExpect(jsonPath("$.items[0].status").value("WAITING"));

        mockMvc.perform(get("/api/v1/inquiries/{inquiryId}", inquiryId.longValue())
                        .with(user(owner.getId().toString()).roles("GUARDIAN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value("EVENT"))
                .andExpect(jsonPath("$.title").value("이벤트 문의"))
                .andExpect(jsonPath("$.content").value("이벤트 적용 조건을 확인하고 싶습니다."))
                .andExpect(jsonPath("$.attachmentUrls[0]")
                        .value("https://example.com/inquiries/image.png"))
                .andExpect(jsonPath("$.status").value("WAITING"));

        assertThat(inquiryRepository.findById(inquiryId.longValue()))
                .hasValueSatisfying(inquiry ->
                        assertThat(inquiry.getStatus()).isEqualTo(InquiryStatus.WAITING));
    }

    @Test
    void userCannotListOrReadAnotherUsersInquiry() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/v1/inquiries")
                        .with(user(owner.getId().toString()).roles("GUARDIAN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_BODY))
                .andReturn();
        Number inquiryId = JsonPath.read(
                createResult.getResponse().getContentAsString(), "$.inquiryId");

        mockMvc.perform(get("/api/v1/inquiries")
                        .with(user(otherUser.getId().toString()).roles("GUARDIAN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());

        mockMvc.perform(get("/api/v1/inquiries/{inquiryId}", inquiryId.longValue())
                        .with(user(otherUser.getId().toString()).roles("GUARDIAN")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("INQUIRY_NOT_FOUND"));
    }

    @Test
    void invalidRequestAndAnonymousRequestAreRejected() throws Exception {
        String invalidBody = """
                {"category":"UNKNOWN","title":"","content":""}
                """;

        mockMvc.perform(post("/api/v1/inquiries")
                        .with(user(owner.getId().toString()).roles("GUARDIAN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));

        mockMvc.perform(get("/api/v1/inquiries")
                        .with(user(owner.getId().toString()).roles("GUARDIAN"))
                        .param("status", "UNKNOWN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));

        mockMvc.perform(get("/api/v1/inquiries"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_001"));
    }
}
