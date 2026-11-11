package com.advancedswiftly.pawfit.business.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.advancedswiftly.pawfit.business.domain.Store;
import com.advancedswiftly.pawfit.business.domain.StoreVerification;
import com.advancedswiftly.pawfit.business.domain.VerificationStatus;
import com.advancedswiftly.pawfit.business.infrastructure.StoreRepository;
import com.advancedswiftly.pawfit.business.infrastructure.StoreVerificationRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@ActiveProfiles("test")
@SpringBootTest
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class BusinessVerificationResubmitControllerTest {

    private static final String NEW_DOCUMENT_URL = "https://example.com/doc-v2.pdf";

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private StoreVerificationRepository verificationRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        // @WithMockUser의 로그인 정보를 요청에 전달하려면 springSecurity()를 직접 적용해야 한다.
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        verificationRepository.deleteAll();
        storeRepository.deleteAll();
    }

    private String url(Long verificationId) {
        return "/api/v1/business/verifications/" + verificationId + "/resubmit";
    }

    private StoreVerification saveVerification(Long userId, String status) {
        Store store = storeRepository.save(Store.registerPending(
                "매장", "02-0000-0000", "서울특별시 성동구", new BigDecimal("37.5446"), new BigDecimal("127.0557")));
        StoreVerification verification = verificationRepository.save(StoreVerification.receive(
                store, userId, "1234567890", "카페", "홍길동", "https://example.com/doc.pdf"));
        jdbcTemplate.update(
                "update store_verification set status = ? where verification_id = ?", status, verification.getId());
        return verification;
    }

    private String body(String documentUrl) {
        return """
                {"documentUrl": "%s"}
                """.formatted(documentUrl);
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void resubmitsAndMovesToReceived() throws Exception {
        StoreVerification verification = saveVerification(1L, "NEEDS_SUPPLEMENT");

        mockMvc.perform(post(url(verification.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(NEW_DOCUMENT_URL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationId").value(verification.getId().intValue()))
                .andExpect(jsonPath("$.status").value("RECEIVED"));

        StoreVerification saved = verificationRepository.findById(verification.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(VerificationStatus.RECEIVED);
        assertThat(saved.getDocumentUrl()).isEqualTo(NEW_DOCUMENT_URL);
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void updatesOnlyProvidedStoreFields() throws Exception {
        StoreVerification verification = saveVerification(1L, "NEEDS_SUPPLEMENT");

        mockMvc.perform(post(url(verification.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"documentUrl": "%s",
                                 "store": {"name": "새 매장명", "latitude": 37.5000000}}
                                """.formatted(NEW_DOCUMENT_URL)))
                .andExpect(status().isOk());

        Store store = storeRepository.findById(verification.getStore().getId()).orElseThrow();
        assertThat(store.getName()).isEqualTo("새 매장명");
        assertThat(store.getLatitude()).isEqualByComparingTo("37.5");
        // 전달하지 않은 항목은 기존 값을 유지한다.
        assertThat(store.getAddress()).isEqualTo("서울특별시 성동구");
        assertThat(store.getPhone()).isEqualTo("02-0000-0000");
        assertThat(store.getLongitude()).isEqualByComparingTo("127.0557");
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void rejectsWhenNotNeedsSupplement() throws Exception {
        StoreVerification verification = saveVerification(1L, "IN_REVIEW");

        mockMvc.perform(post(url(verification.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(NEW_DOCUMENT_URL)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("RESUBMIT_NOT_ALLOWED"));

        StoreVerification saved = verificationRepository.findById(verification.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(VerificationStatus.IN_REVIEW);
        assertThat(saved.getDocumentUrl()).isEqualTo("https://example.com/doc.pdf");
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void othersVerificationIsNotFound() throws Exception {
        StoreVerification verification = saveVerification(2L, "NEEDS_SUPPLEMENT");

        mockMvc.perform(post(url(verification.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(NEW_DOCUMENT_URL)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMON_002"));
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void unknownVerificationIsNotFound() throws Exception {
        mockMvc.perform(post(url(999999L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(NEW_DOCUMENT_URL)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void missingDocumentUrlReturnsBadRequest() throws Exception {
        StoreVerification verification = saveVerification(1L, "NEEDS_SUPPLEMENT");

        mockMvc.perform(post(url(verification.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void invalidStoreFieldReturnsBadRequest() throws Exception {
        StoreVerification verification = saveVerification(1L, "NEEDS_SUPPLEMENT");

        mockMvc.perform(post(url(verification.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"documentUrl": "%s", "store": {"latitude": 123.0}}
                                """.formatted(NEW_DOCUMENT_URL)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));
    }

    @Test
    @WithMockUser(username = "1", roles = "GUARDIAN")
    void guardianCannotResubmit() throws Exception {
        mockMvc.perform(post(url(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(NEW_DOCUMENT_URL)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_002"));
    }

    @Test
    void anonymousUserIsUnauthorized() throws Exception {
        mockMvc.perform(post(url(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(NEW_DOCUMENT_URL)))
                .andExpect(status().isUnauthorized());
    }
}
