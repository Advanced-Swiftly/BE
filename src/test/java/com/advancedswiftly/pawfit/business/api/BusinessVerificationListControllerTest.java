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
class BusinessVerificationListControllerTest {

    private static final String URL = "/api/v1/business/verifications";

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

    private StoreVerification saveVerification(Long userId, String businessNumber, String businessName) {
        Store store = storeRepository.save(Store.registerPending(
                "매장", "02-0000-0000", "서울특별시 성동구", new BigDecimal("37.5446"), new BigDecimal("127.0557")));
        return verificationRepository.save(StoreVerification.receive(
                store, userId, businessNumber, businessName, "홍길동", "https://example.com/doc.pdf"));
    }

    private void changeStatus(Long verificationId, String status) {
        jdbcTemplate.update(
                "update store_verification set status = ? where verification_id = ?", status, verificationId);
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void returnsOnlyMyVerificationsNewestFirst() throws Exception {
        saveVerification(1L, "1111111111", "첫번째 카페");
        StoreVerification newest = saveVerification(1L, "2222222222", "두번째 카페");
        saveVerification(2L, "3333333333", "다른 사람의 카페");

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].verificationId").value(newest.getId().intValue()))
                .andExpect(jsonPath("$.items[0].status").value("RECEIVED"))
                .andExpect(jsonPath("$.items[0].businessName").value("두번째 카페"))
                .andExpect(jsonPath("$.items[1].businessName").value("첫번째 카페"));
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void filtersByStatus() throws Exception {
        StoreVerification inReview = saveVerification(1L, "1111111111", "심사중 카페");
        saveVerification(1L, "2222222222", "접수 카페");
        changeStatus(inReview.getId(), "IN_REVIEW");

        mockMvc.perform(get(URL).param("status", "IN_REVIEW"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].verificationId").value(inReview.getId().intValue()))
                .andExpect(jsonPath("$.items[0].status").value("IN_REVIEW"));
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void blankStatusReturnsAll() throws Exception {
        saveVerification(1L, "1111111111", "첫번째 카페");
        saveVerification(1L, "2222222222", "두번째 카페");

        mockMvc.perform(get(URL).param("status", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2));
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void returnsEmptyItemsWhenNoVerification() throws Exception {
        saveVerification(2L, "3333333333", "다른 사람의 카페");

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void invalidStatusReturnsBadRequest() throws Exception {
        mockMvc.perform(get(URL).param("status", "UNKNOWN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));
    }

    @Test
    @WithMockUser(username = "1", roles = "GUARDIAN")
    void guardianCannotListVerifications() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_002"));
    }

    @Test
    void anonymousUserIsUnauthorized() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isUnauthorized());
    }
}
