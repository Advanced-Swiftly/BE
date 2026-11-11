package com.advancedswiftly.pawfit.business.api;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.advancedswiftly.pawfit.business.domain.Store;
import com.advancedswiftly.pawfit.business.domain.StoreVerification;
import com.advancedswiftly.pawfit.business.infrastructure.StoreRepository;
import com.advancedswiftly.pawfit.business.infrastructure.StoreVerificationRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
class BusinessVerificationDetailControllerTest {

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
        return "/api/v1/business/verifications/" + verificationId;
    }

    private StoreVerification saveVerification(Long userId) {
        Store store = storeRepository.save(Store.registerPending(
                "매장", "02-0000-0000", "서울특별시 성동구", new BigDecimal("37.5446"), new BigDecimal("127.0557")));
        return verificationRepository.save(StoreVerification.receive(
                store, userId, "1234567890", "카페", "홍길동", "https://example.com/doc.pdf"));
    }

    private void review(Long verificationId, String status, String note) {
        jdbcTemplate.update(
                "update store_verification set status = ?, review_note = ? where verification_id = ?",
                status, note, verificationId);
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void returnsStatusAndReviewComment() throws Exception {
        StoreVerification verification = saveVerification(1L);
        review(verification.getId(), "NEEDS_SUPPLEMENT", "등록증 식별이 어렵습니다.");

        mockMvc.perform(get(url(verification.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationId").value(verification.getId().intValue()))
                .andExpect(jsonPath("$.status").value("NEEDS_SUPPLEMENT"))
                .andExpect(jsonPath("$.reviewComment").value("등록증 식별이 어렵습니다."));
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void omitsReviewCommentBeforeReview() throws Exception {
        StoreVerification verification = saveVerification(1L);

        mockMvc.perform(get(url(verification.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECEIVED"))
                // 전역 설정(default-property-inclusion: non_null)에 따라 값이 없으면 필드가 응답에서 빠진다.
                .andExpect(jsonPath("$.reviewComment").doesNotExist());
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void othersVerificationIsNotFound() throws Exception {
        StoreVerification verification = saveVerification(2L);

        mockMvc.perform(get(url(verification.getId())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMON_002"));
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void unknownVerificationIsNotFound() throws Exception {
        mockMvc.perform(get(url(999999L)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "1", roles = "GUARDIAN")
    void guardianCannotView() throws Exception {
        mockMvc.perform(get(url(1L)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_002"));
    }

    @Test
    void anonymousUserIsUnauthorized() throws Exception {
        mockMvc.perform(get(url(1L)))
                .andExpect(status().isUnauthorized());
    }
}
