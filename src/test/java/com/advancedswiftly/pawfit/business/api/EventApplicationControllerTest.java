package com.advancedswiftly.pawfit.business.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.advancedswiftly.pawfit.business.domain.ApplicationStatus;
import com.advancedswiftly.pawfit.business.domain.EventApplication;
import com.advancedswiftly.pawfit.business.domain.Store;
import com.advancedswiftly.pawfit.business.domain.StoreVerification;
import com.advancedswiftly.pawfit.business.infrastructure.EventApplicationRepository;
import com.advancedswiftly.pawfit.business.infrastructure.StoreRepository;
import com.advancedswiftly.pawfit.business.infrastructure.StoreVerificationRepository;
import java.math.BigDecimal;
import java.util.Set;
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
class EventApplicationControllerTest {

    private static final String URL = "/api/v1/business/event-applications";
    private static final long EVENT_ID = 9001L;
    private static final long CLOSED_EVENT_ID = 9002L;

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private StoreVerificationRepository verificationRepository;

    @Autowired
    private EventApplicationRepository applicationRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        // @WithMockUser의 로그인 정보를 요청에 전달하려면 springSecurity()를 직접 적용해야 한다.
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        applicationRepository.deleteAll();
        verificationRepository.deleteAll();
        storeRepository.deleteAll();
        jdbcTemplate.update("delete from event_region where event_id in (?, ?)", EVENT_ID, CLOSED_EVENT_ID);
        jdbcTemplate.update("delete from stamp_event where event_id in (?, ?)", EVENT_ID, CLOSED_EVENT_ID);
        insertEvent(EVENT_ID, "OPEN");
        insertEvent(CLOSED_EVENT_ID, "CLOSED");
    }

    private void insertEvent(long eventId, String status) {
        jdbcTemplate.update(
                "insert into stamp_event (event_id, title, event_year, quarter, status, starts_on, ends_on,"
                        + " required_stamp_count, max_stamp_count) values (?, ?, 2026, 4, ?, '2026-10-01',"
                        + " '2026-12-31', 2, 10)",
                eventId, "테스트 이벤트", status);
        jdbcTemplate.update("insert into event_region (event_id, region_id) values (?, 11), (?, 41)", eventId, eventId);
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

    private EventApplication saveApplication(Long userId, Long storeId, String status) {
        EventApplication application =
                applicationRepository.save(EventApplication.receive(storeId, EVENT_ID, userId, Set.of(11L), null));
        jdbcTemplate.update(
                "update event_application set status = ? where application_id = ?", status, application.getId());
        return application;
    }

    private String applyBody(Long storeId, long eventId, String regionIds) {
        return """
                {"storeId": %d, "eventId": %d, "regionIds": %s, "message": "참여 희망"}
                """.formatted(storeId, eventId, regionIds);
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void appliesToEvent() throws Exception {
        Long storeId = saveManagedStore(1L);

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(applyBody(storeId, EVENT_ID, "[11, 41]")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.applicationId").isNumber())
                .andExpect(jsonPath("$.status").value("RECEIVED"));

        assertThat(applicationRepository.findAll()).hasSize(1);
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void rejectsDuplicateActiveApplication() throws Exception {
        Long storeId = saveManagedStore(1L);
        saveApplication(1L, storeId, "RECEIVED");

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(applyBody(storeId, EVENT_ID, "[11]")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("DUPLICATE_APPLICATION"));
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void canReapplyAfterCanceled() throws Exception {
        Long storeId = saveManagedStore(1L);
        saveApplication(1L, storeId, "CANCELED");

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(applyBody(storeId, EVENT_ID, "[11]")))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void rejectsStoreNotManagedByUser() throws Exception {
        Long othersStoreId = saveManagedStore(2L);

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(applyBody(othersStoreId, EVENT_ID, "[11]")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_002"));
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void rejectsRegionOutsideEvent() throws Exception {
        Long storeId = saveManagedStore(1L);

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(applyBody(storeId, EVENT_ID, "[11, 99]")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void rejectsClosedEvent() throws Exception {
        Long storeId = saveManagedStore(1L);

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(applyBody(storeId, CLOSED_EVENT_ID, "[11]")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void unknownEventIsNotFound() throws Exception {
        Long storeId = saveManagedStore(1L);

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(applyBody(storeId, 999999L, "[11]")))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void emptyRegionIdsReturnsBadRequest() throws Exception {
        Long storeId = saveManagedStore(1L);

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(applyBody(storeId, EVENT_ID, "[]")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void listsOnlyMyApplicationsWithFilters() throws Exception {
        Long storeId = saveManagedStore(1L);
        Long otherStoreId = saveManagedStore(1L);
        EventApplication approved = saveApplication(1L, storeId, "APPROVED");
        saveApplication(1L, otherStoreId, "RECEIVED");
        saveApplication(2L, saveManagedStore(2L), "RECEIVED");

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2));

        mockMvc.perform(get(URL).param("storeId", String.valueOf(storeId)).param("status", "APPROVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].applicationId").value(approved.getId().intValue()))
                .andExpect(jsonPath("$.items[0].eventId").value((int) EVENT_ID))
                .andExpect(jsonPath("$.items[0].status").value("APPROVED"));
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
        EventApplication application = saveApplication(1L, saveManagedStore(1L), "RECEIVED");

        mockMvc.perform(get(URL + "/" + application.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationId").value(application.getId().intValue()))
                .andExpect(jsonPath("$.status").value("RECEIVED"))
                .andExpect(jsonPath("$.reviewComment").doesNotExist());
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void returnsReviewCommentInDetail() throws Exception {
        EventApplication application = saveApplication(1L, saveManagedStore(1L), "APPROVED");
        jdbcTemplate.update(
                "update event_application set review_note = ? where application_id = ?",
                "참여가 승인되었습니다.", application.getId());

        mockMvc.perform(get(URL + "/" + application.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewComment").value("참여가 승인되었습니다."));
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void othersApplicationDetailIsNotFound() throws Exception {
        EventApplication application = saveApplication(2L, saveManagedStore(2L), "RECEIVED");

        mockMvc.perform(get(URL + "/" + application.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMON_002"));
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void cancelsBeforeReview() throws Exception {
        EventApplication application = saveApplication(1L, saveManagedStore(1L), "RECEIVED");

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
        EventApplication application = saveApplication(1L, saveManagedStore(1L), "IN_REVIEW");

        mockMvc.perform(patch(URL + "/" + application.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\": \"CANCEL\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CANCEL_NOT_ALLOWED"));
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void cancelWithInvalidActionReturnsBadRequest() throws Exception {
        EventApplication application = saveApplication(1L, saveManagedStore(1L), "RECEIVED");

        mockMvc.perform(patch(URL + "/" + application.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\": \"DELETE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void cannotCancelOthersApplication() throws Exception {
        EventApplication application = saveApplication(2L, saveManagedStore(2L), "RECEIVED");

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
