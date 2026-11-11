package com.advancedswiftly.pawfit.business.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.advancedswiftly.pawfit.business.domain.AdApplication;
import com.advancedswiftly.pawfit.business.domain.AdType;
import com.advancedswiftly.pawfit.business.domain.ApplicationStatus;
import com.advancedswiftly.pawfit.business.domain.Store;
import com.advancedswiftly.pawfit.business.domain.StoreVerification;
import com.advancedswiftly.pawfit.business.infrastructure.AdApplicationRepository;
import com.advancedswiftly.pawfit.business.infrastructure.StoreRepository;
import com.advancedswiftly.pawfit.business.infrastructure.StoreVerificationRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
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
class AdApplicationControllerTest {

    private static final String URL = "/api/v1/business/ad-applications";
    private static final String CREATIVE_URL = "https://cdn.pawfit.app/ad/1601.png";

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private StoreVerificationRepository verificationRepository;

    @Autowired
    private AdApplicationRepository applicationRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        // @WithMockUser의 로그인 정보를 요청에 전달하려면 springSecurity()를 직접 적용해야 한다.
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        applicationRepository.deleteAll();
        verificationRepository.deleteAll();
        storeRepository.deleteAll();
    }

    /** userId가 관리하는(인증이 승인된) 업체를 만든다. */
    private Long saveManagedStore(Long userId) {
        Store store = storeRepository.save(Store.registerPending(
                "매장", "02-0000-0000", "서울특별시 성동구", new BigDecimal("37.5446"), new BigDecimal("127.0557")));
        StoreVerification verification = verificationRepository.save(StoreVerification.receive(
                store, userId, String.valueOf(1000000000L + store.getId()), "카페", "홍길동", "https://example.com/doc.pdf"));
        jdbcTemplate.update(
                "update store_verification set status = 'APPROVED' where verification_id = ?", verification.getId());
        return store.getId();
    }

    private AdApplication saveApplication(Long userId, Long storeId, AdType adType, String status) {
        AdApplication application = applicationRepository.save(AdApplication.receive(
                storeId, userId, adType, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 31), CREATIVE_URL, null));
        jdbcTemplate.update(
                "update ad_application set status = ? where application_id = ?", status, application.getId());
        return application;
    }

    private String applyBody(Long storeId, String startDate, String endDate) {
        return """
                {"storeId": %d, "adType": "BANNER", "startDate": "%s", "endDate": "%s",
                 "creativeUrl": "%s", "message": "연말 프로모션"}
                """.formatted(storeId, startDate, endDate, CREATIVE_URL);
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void appliesForAd() throws Exception {
        Long storeId = saveManagedStore(1L);

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(applyBody(storeId, "2026-12-01", "2026-12-31")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.applicationId").isNumber())
                .andExpect(jsonPath("$.status").value("RECEIVED"));

        assertThat(applicationRepository.findAll()).hasSize(1);
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void acceptsSingleDayPeriod() throws Exception {
        Long storeId = saveManagedStore(1L);

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(applyBody(storeId, "2026-12-01", "2026-12-01")))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void rejectsEndDateBeforeStartDate() throws Exception {
        Long storeId = saveManagedStore(1L);

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(applyBody(storeId, "2026-12-31", "2026-12-01")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void rejectsStoreNotManagedByUser() throws Exception {
        Long othersStoreId = saveManagedStore(2L);

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(applyBody(othersStoreId, "2026-12-01", "2026-12-31")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_002"));
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void rejectsInvalidAdType() throws Exception {
        Long storeId = saveManagedStore(1L);

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(applyBody(storeId, "2026-12-01", "2026-12-31").replace("BANNER", "POPUP")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void rejectsInvalidCreativeUrl() throws Exception {
        Long storeId = saveManagedStore(1L);

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(applyBody(storeId, "2026-12-01", "2026-12-31").replace(CREATIVE_URL, "not-a-url")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void listsOnlyMyApplicationsWithFilters() throws Exception {
        Long storeId = saveManagedStore(1L);
        Long otherStoreId = saveManagedStore(1L);
        AdApplication inReview = saveApplication(1L, storeId, AdType.BANNER, "IN_REVIEW");
        saveApplication(1L, otherStoreId, AdType.RECOMMENDED, "RECEIVED");
        saveApplication(2L, saveManagedStore(2L), AdType.BANNER, "RECEIVED");

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2));

        mockMvc.perform(get(URL).param("storeId", String.valueOf(storeId)).param("status", "IN_REVIEW"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].applicationId").value(inReview.getId().intValue()))
                .andExpect(jsonPath("$.items[0].adType").value("BANNER"))
                .andExpect(jsonPath("$.items[0].status").value("IN_REVIEW"));
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void invalidStatusFilterReturnsBadRequest() throws Exception {
        mockMvc.perform(get(URL).param("status", "UNKNOWN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void returnsDetail() throws Exception {
        AdApplication application = saveApplication(1L, saveManagedStore(1L), AdType.BANNER, "IN_REVIEW");

        mockMvc.perform(get(URL + "/" + application.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationId").value(application.getId().intValue()))
                .andExpect(jsonPath("$.status").value("IN_REVIEW"))
                .andExpect(jsonPath("$.creativeUrl").value(CREATIVE_URL));
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void othersApplicationDetailIsNotFound() throws Exception {
        AdApplication application = saveApplication(2L, saveManagedStore(2L), AdType.BANNER, "RECEIVED");

        mockMvc.perform(get(URL + "/" + application.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMON_002"));
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void cancelsBeforeReview() throws Exception {
        AdApplication application = saveApplication(1L, saveManagedStore(1L), AdType.BANNER, "RECEIVED");

        mockMvc.perform(patch(URL + "/" + application.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\": \"CANCEL\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationId").value(application.getId().intValue()))
                .andExpect(jsonPath("$.status").value("CANCELED"));

        assertThat(applicationRepository.findById(application.getId()).orElseThrow().getStatus())
                .isEqualTo(ApplicationStatus.CANCELED);
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void cannotCancelAfterReviewStarted() throws Exception {
        AdApplication application = saveApplication(1L, saveManagedStore(1L), AdType.BANNER, "APPROVED");

        mockMvc.perform(patch(URL + "/" + application.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\": \"CANCEL\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CANCEL_NOT_ALLOWED"));
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void cannotCancelOthersApplication() throws Exception {
        AdApplication application = saveApplication(2L, saveManagedStore(2L), AdType.BANNER, "RECEIVED");

        mockMvc.perform(patch(URL + "/" + application.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\": \"CANCEL\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "1", roles = "GUARDIAN")
    void guardianIsForbidden() throws Exception {
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
