package com.advancedswiftly.pawfit.business.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.advancedswiftly.pawfit.business.domain.Store;
import com.advancedswiftly.pawfit.business.domain.StoreVerification;
import com.advancedswiftly.pawfit.business.domain.StoreVerificationStatus;
import com.advancedswiftly.pawfit.business.domain.VerificationStatus;
import com.advancedswiftly.pawfit.business.infrastructure.StoreRepository;
import com.advancedswiftly.pawfit.business.infrastructure.StoreVerificationRepository;
import java.util.List;
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
class BusinessVerificationControllerTest {

    private static final String URL = "/api/v1/business/verifications";

    private static final String BODY_TEMPLATE = """
            {
              "businessNumber": "%s",
              "businessName": "성수 댕댕카페",
              "representativeName": "홍길동",
              "documentUrl": "https://example.com/documents/business-registration.pdf",
              "store": {
                "name": "성수 댕댕카페",
                "address": "서울특별시 성동구 성수이로 1",
                "phone": "02-1234-5678",
                "latitude": %s,
                "longitude": 127.0557
              }
            }
            """;

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

    private static String body(String businessNumber) {
        return BODY_TEMPLATE.formatted(businessNumber, "37.5446");
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void requestVerificationCreatesVerificationAndPendingStore() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body("123-45-67890")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.verificationId").isNumber())
                .andExpect(jsonPath("$.status").value("RECEIVED"));

        List<StoreVerification> verifications = verificationRepository.findAll();
        assertThat(verifications).hasSize(1);
        StoreVerification verification = verifications.get(0);
        assertThat(verification.getBusinessNumber()).isEqualTo("1234567890");
        assertThat(verification.getSubmittedBy()).isEqualTo(1L);
        assertThat(verification.getStatus()).isEqualTo(VerificationStatus.RECEIVED);

        List<Store> stores = storeRepository.findAll();
        assertThat(stores).hasSize(1);
        assertThat(stores.get(0).getVerificationStatus()).isEqualTo(StoreVerificationStatus.PENDING);
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void duplicateBusinessNumberIsRejectedEvenWithoutHyphens() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body("123-45-67890")))
                .andExpect(status().isCreated());

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body("1234567890")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("DUPLICATE_BUSINESS_NUMBER"))
                .andExpect(jsonPath("$.message").value("요청을 처리할 수 없습니다."));

        assertThat(verificationRepository.count()).isEqualTo(1);
        assertThat(storeRepository.count()).isEqualTo(1);
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void rejectedBusinessNumberCanBeRequestedAgain() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body("123-45-67890")))
                .andExpect(status().isCreated());
        jdbcTemplate.update("update store_verification set status = 'REJECTED'");

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body("123-45-67890")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("RECEIVED"));

        assertThat(verificationRepository.count()).isEqualTo(2);
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void invalidBusinessNumberReturnsFieldError() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body("12345")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"))
                .andExpect(jsonPath("$.fieldErrors.businessNumber").exists());

        assertThat(verificationRepository.count()).isZero();
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void missingStoreReturnsFieldError() throws Exception {
        String bodyWithoutStore = """
                {
                  "businessNumber": "123-45-67890",
                  "businessName": "성수 댕댕카페",
                  "representativeName": "홍길동",
                  "documentUrl": "https://example.com/documents/business-registration.pdf"
                }
                """;

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(bodyWithoutStore))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.store").exists());
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void latitudeOutOfRangeReturnsFieldError() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_TEMPLATE.formatted("123-45-67890", "123.5")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors['store.latitude']").exists());
    }

    @Test
    @WithMockUser(username = "1", roles = "GUARDIAN")
    void guardianCannotRequestVerification() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body("123-45-67890")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_002"));

        assertThat(verificationRepository.count()).isZero();
    }

    @Test
    void anonymousUserIsUnauthorized() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body("123-45-67890")))
                .andExpect(status().isUnauthorized());
    }
}
