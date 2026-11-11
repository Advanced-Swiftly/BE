package com.advancedswiftly.pawfit.admin.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.advancedswiftly.pawfit.auth.domain.User;
import com.advancedswiftly.pawfit.auth.domain.UserRole;
import com.advancedswiftly.pawfit.auth.infrastructure.UserRepository;
import com.advancedswiftly.pawfit.business.domain.ConditionRequest;
import com.advancedswiftly.pawfit.business.domain.PolicyRule;
import com.advancedswiftly.pawfit.business.domain.PolicyRuleType;
import com.advancedswiftly.pawfit.business.domain.Store;
import com.advancedswiftly.pawfit.business.domain.StorePolicy;
import com.advancedswiftly.pawfit.business.domain.StoreVerification;
import com.advancedswiftly.pawfit.business.infrastructure.ConditionRequestRepository;
import com.advancedswiftly.pawfit.business.infrastructure.StorePolicyRepository;
import com.advancedswiftly.pawfit.business.infrastructure.StoreRepository;
import com.advancedswiftly.pawfit.business.infrastructure.StoreVerificationRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
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
class AdminStoreManagementControllerTest {

    private static final String BASE = "/api/v1/admin";

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private StoreVerificationRepository verificationRepository;

    @Autowired
    private StorePolicyRepository policyRepository;

    @Autowired
    private ConditionRequestRepository conditionRequestRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        for (String table : List.of(
                "event_draw", "draw_entry_fulfillment", "draw_entry", "event_stamp", "event_store", "event_prize",
                "event_region", "stamp_event", "store_manager", "store_verification_history", "store_qr",
                "store_policy_rule", "store_policy", "condition_request_rule", "condition_request_evidence",
                "condition_request", "store_verification", "store")) {
            jdbcTemplate.update("delete from " + table);
        }
        jdbcTemplate.update("merge into region (region_id, name, active) key (region_id) values (11, '서울특별시', true)");
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
    }

    private Store saveStore(String name) {
        return storeRepository.save(Store.registerPending(
                name, "02-0000-0000", "서울특별시 성동구", new BigDecimal("37.5446"), new BigDecimal("127.0557")));
    }

    private StoreVerification saveVerification(Store store, long userId, String status) {
        StoreVerification verification = verificationRepository.save(StoreVerification.receive(
                store, userId, String.valueOf(1000000000L + store.getId()), "상호" + store.getId(), "홍길동",
                "docs/license.pdf"));
        jdbcTemplate.update(
                "update store_verification set status = ? where verification_id = ?", status, verification.getId());
        return verification;
    }

    private User saveUser(UserRole role) {
        return userRepository.save(User.register("010" + System.nanoTime() % 100000000L, "hash", "관리자", role));
    }


    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void rejectsNonOperators() throws Exception {
        mockMvc.perform(get(BASE + "/stores")).andExpect(status().isForbidden());
        mockMvc.perform(get(BASE + "/business-verifications")).andExpect(status().isForbidden());
        mockMvc.perform(get(BASE + "/condition-requests")).andExpect(status().isForbidden());
    }

    // ---- 사업자 인증 심사

    @Test
    @WithMockUser(username = "9", roles = "OPERATOR")
    void listsBusinessVerifications() throws Exception {
        saveVerification(saveStore("멍카페"), 2L, "RECEIVED");
        saveVerification(saveStore("냥카페"), 3L, "NEEDS_SUPPLEMENT");

        mockMvc.perform(get(BASE + "/business-verifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2));
        mockMvc.perform(get(BASE + "/business-verifications").param("status", "NEEDS_INFO"))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].status").value("NEEDS_SUPPLEMENT"));
        mockMvc.perform(get(BASE + "/business-verifications").param("keyword", "상호"))
                .andExpect(jsonPath("$.items.length()").value(2));
        mockMvc.perform(get(BASE + "/business-verifications").param("keyword", "zzz"))
                .andExpect(jsonPath("$.items.length()").value(0));
        mockMvc.perform(get(BASE + "/business-verifications").param("status", "BOGUS"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "9", roles = "OPERATOR")
    void reviewsVerificationThroughApproval() throws Exception {
        Store store = saveStore("멍카페");
        StoreVerification verification = saveVerification(store, 2L, "RECEIVED");
        String url = BASE + "/business-verifications/" + verification.getId();

        mockMvc.perform(patch(url).contentType(MediaType.APPLICATION_JSON).content("{\"action\":\"APPROVE\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));
        mockMvc.perform(patch(url).contentType(MediaType.APPLICATION_JSON).content("{\"action\":\"START_REVIEW\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_REVIEW"));
        mockMvc.perform(patch(url).contentType(MediaType.APPLICATION_JSON).content("{\"action\":\"NEEDS_INFO\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch(url).contentType(MediaType.APPLICATION_JSON).content("{\"action\":\"APPROVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.storeId").value(store.getId()));

        assertThat(jdbcTemplate.queryForObject(
                        "select verification_status from store where store_id = ?", String.class, store.getId()))
                .isEqualTo("VERIFIED");
        assertThat(jdbcTemplate.queryForObject(
                        "select user_id from store_manager where store_id = ?", Long.class, store.getId()))
                .isEqualTo(2L);

        mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.storeName").value("멍카페"))
                .andExpect(jsonPath("$.statusHistory.length()").value(3))
                .andExpect(jsonPath("$.statusHistory[0].status").value("RECEIVED"))
                .andExpect(jsonPath("$.statusHistory[2].status").value("APPROVED"));
        mockMvc.perform(get(BASE + "/business-verifications/9999")).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "9", roles = "OPERATOR")
    void rejectingVerificationMarksStoreRejected() throws Exception {
        Store store = saveStore("멍카페");
        StoreVerification verification = saveVerification(store, 2L, "IN_REVIEW");
        mockMvc.perform(patch(BASE + "/business-verifications/" + verification.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"REJECT\",\"comment\":\"서류 불일치\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
        assertThat(jdbcTemplate.queryForObject(
                        "select verification_status from store where store_id = ?", String.class, store.getId()))
                .isEqualTo("REJECTED");
        assertThat(jdbcTemplate.queryForObject("select count(*) from store_manager", Long.class)).isZero();
    }

    // ---- 업체 관리

    @Test
    @WithMockUser(username = "9", roles = "OPERATOR")
    void createsStoreHiddenAndListsIt() throws Exception {
        String body = """
                {"name":"새 카페","address":"서울 성동구","latitude":37.5,"longitude":127.0,
                 "phoneNumber":"02-1111-2222","regionId":11}""";
        mockMvc.perform(post(BASE + "/stores").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.visibility").value("HIDDEN"));
        mockMvc.perform(post(BASE + "/stores").contentType(MediaType.APPLICATION_JSON)
                        .content(body.replace("\"regionId\":11", "\"regionId\":999")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(BASE + "/stores").contentType(MediaType.APPLICATION_JSON)
                        .content(body.replace("37.5", "137.5")))
                .andExpect(status().isBadRequest());

        saveStore("보이는 카페");
        mockMvc.perform(get(BASE + "/stores").param("visibility", "HIDDEN"))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].name").value("새 카페"))
                .andExpect(jsonPath("$.items[0].regionId").value(11));
        mockMvc.perform(get(BASE + "/stores").param("keyword", "카페"))
                .andExpect(jsonPath("$.items.length()").value(2));
        mockMvc.perform(get(BASE + "/stores").param("regionId", "11"))
                .andExpect(jsonPath("$.items.length()").value(1));
        mockMvc.perform(get(BASE + "/stores").param("visibility", "BOGUS")).andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "9", roles = "OPERATOR")
    void showsStoreDetailWithManagerAndPendingRequests() throws Exception {
        Store store = saveStore("멍카페");
        User manager = saveUser(UserRole.BUSINESS);
        jdbcTemplate.update("insert into store_manager (store_id, user_id, created_at, updated_at) values (?, ?, now(), now())",
                store.getId(), manager.getId());
        policyRepository.save(StorePolicy.create(store.getId(), 3, "정책", List.of()));
        conditionRequestRepository.save(ConditionRequest.receive(store.getId(), 3, "원문", 1L, List.of(), List.of()));

        mockMvc.perform(get(BASE + "/stores/" + store.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("멍카페"))
                .andExpect(jsonPath("$.manager.userId").value(manager.getId()))
                .andExpect(jsonPath("$.manager.name").value("관리자"))
                .andExpect(jsonPath("$.policyVersion").value(3))
                .andExpect(jsonPath("$.pendingRequests").value(1))
                .andExpect(jsonPath("$.qrStatus").doesNotExist());
        mockMvc.perform(get(BASE + "/stores/9999")).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "9", roles = "OPERATOR")
    void updatesStoreAndReplacesManager() throws Exception {
        Store store = saveStore("멍카페");
        User business = saveUser(UserRole.BUSINESS);
        User guardian = saveUser(UserRole.GUARDIAN);
        String url = BASE + "/stores/" + store.getId();

        mockMvc.perform(patch(url).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"새 이름\",\"description\":\"소개\",\"managerUserId\":" + business.getId() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("새 이름"));
        assertThat(jdbcTemplate.queryForObject(
                        "select user_id from store_manager where store_id = ?", Long.class, store.getId()))
                .isEqualTo(business.getId());
        assertThat(jdbcTemplate.queryForObject(
                        "select description from store where store_id = ?", String.class, store.getId()))
                .isEqualTo("소개");

        mockMvc.perform(patch(url).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"managerUserId\":" + guardian.getId() + "}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch(url).contentType(MediaType.APPLICATION_JSON).content("{\"managerUserId\":999999}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch(BASE + "/stores/9999").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "9", roles = "OPERATOR")
    void changesVisibility() throws Exception {
        Store store = saveStore("멍카페");
        String url = BASE + "/stores/" + store.getId() + "/visibility";
        mockMvc.perform(patch(url).contentType(MediaType.APPLICATION_JSON).content("{\"visible\":false,\"reason\":\"폐업 의심\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visibility").value("HIDDEN"));
        mockMvc.perform(patch(url).contentType(MediaType.APPLICATION_JSON).content("{\"visible\":true}"))
                .andExpect(jsonPath("$.visibility").value("VISIBLE"));
        mockMvc.perform(patch(url).contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isBadRequest());
        mockMvc.perform(patch(BASE + "/stores/9999/visibility").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"visible\":true}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "9", roles = "OPERATOR")
    void createsRecheckAndRequiresPolicyOnApproval() throws Exception {
        Store store = saveStore("멍카페");
        policyRepository.save(StorePolicy.create(
                store.getId(), 1, "초기", List.of(new PolicyRule(PolicyRuleType.EQUIPMENT, "CARRIER", false))));

        String body = mockMvc.perform(post(BASE + "/stores/" + store.getId() + "/recheck")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"현장 확인\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.kind").value("RECHECK"))
                .andExpect(jsonPath("$.status").value("RECEIVED"))
                .andReturn().getResponse().getContentAsString();
        long requestId = com.jayway.jsonpath.JsonPath.parse(body).read("$.requestId", Long.class);
        mockMvc.perform(post(BASE + "/stores/" + store.getId() + "/recheck")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(BASE + "/stores/9999/recheck")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"x\"}"))
                .andExpect(status().isNotFound());

        String url = BASE + "/condition-requests/" + requestId;
        mockMvc.perform(patch(url).contentType(MediaType.APPLICATION_JSON).content("{\"action\":\"START_REVIEW\"}"))
                .andExpect(status().isOk());
        // 재검토 요청은 요청한 정책이 없으므로 적용할 정책 없이는 승인할 수 없다.
        mockMvc.perform(patch(url).contentType(MediaType.APPLICATION_JSON).content("{\"action\":\"APPROVE\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch(url).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"APPROVE\",\"approvedPolicy\":{\"equipmentRules\":[{\"code\":\"CARRIER\",\"required\":true}]}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appliedPolicyVersion").value(2));
    }

    @Test
    @WithMockUser(username = "9", roles = "OPERATOR")
    void listsConditionRequestsByKind() throws Exception {
        Store store = saveStore("멍카페");
        conditionRequestRepository.save(ConditionRequest.receive(store.getId(), 0, "원문", 1L, List.of(), List.of()));
        conditionRequestRepository.save(ConditionRequest.recheck(store.getId(), 0, "사유", 9L));
        conditionRequestRepository.save(ConditionRequest.operatorDirect(store.getId(), 0, "원문", 9L, List.of(), "사유"));

        mockMvc.perform(get(BASE + "/condition-requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(3));
        mockMvc.perform(get(BASE + "/condition-requests").param("kind", "RECHECK"))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].kind").value("RECHECK"));
        mockMvc.perform(get(BASE + "/condition-requests").param("status", "APPROVED"))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].kind").value("OPERATOR_DIRECT"));
        mockMvc.perform(get(BASE + "/condition-requests").param("storeId", "9999"))
                .andExpect(jsonPath("$.items.length()").value(0));
        mockMvc.perform(get(BASE + "/condition-requests").param("kind", "BOGUS")).andExpect(status().isBadRequest());
    }

    // ---- 이벤트 상태·추첨

    private void insertEvent(long eventId, String status, int year) {
        jdbcTemplate.update(
                "insert into stamp_event (event_id, title, event_year, quarter, status, starts_on, ends_on,"
                        + " required_stamp_count, max_stamp_count) values (?, '이벤트', ?, 1, ?,"
                        + " '2026-01-01', '2026-03-31', 2, 5)",
                eventId, year, status);
    }

    @Test
    @WithMockUser(username = "9", roles = "OPERATOR")
    void changesEventStatusAlongAllowedTransitions() throws Exception {
        insertEvent(801, "DRAFT", 2026);
        String url = BASE + "/events/801/status";

        mockMvc.perform(patch(url).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CLOSED\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));
        // 지역·경품 없이 공개할 수 없다.
        mockMvc.perform(patch(url).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"OPEN\"}"))
                .andExpect(status().isBadRequest());
        jdbcTemplate.update("insert into event_region (event_id, region_id) values (801, 11)");
        jdbcTemplate.update("insert into event_prize (event_id, prize_rank, name, quantity) values (801, 1, '상품', 1)");
        mockMvc.perform(patch(url).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"OPEN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"));
        mockMvc.perform(patch(url).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CLOSED\"}"))
                .andExpect(status().isOk());
        // 추첨 전에는 추첨 완료로 바꿀 수 없다.
        mockMvc.perform(patch(url).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DRAWN\"}"))
                .andExpect(status().isConflict());
        mockMvc.perform(patch(url).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DRAFT\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch(url).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"BOGUS\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch(BASE + "/events/9999/status").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"OPEN\"}"))
                .andExpect(status().isNotFound());
    }

    private void insertEntry(long userId) {
        jdbcTemplate.update(
                "insert into draw_entry (event_id, user_id, phone_number, address, privacy_agreed, status, created_at)"
                        + " values (801, ?, '010', '서울', true, 'ENTERED', now())",
                userId);
    }

    private void insertStamps(long userId, int count) {
        for (int i = 0; i < count; i++) {
            jdbcTemplate.update(
                    "insert into event_stamp (event_id, user_id, store_id, awarded_at) values (801, ?, ?, now())",
                    userId, 100 + i);
        }
    }

    @Test
    @WithMockUser(username = "9", roles = "OPERATOR")
    void drawsWinnersOnceWithReproducibleSeed() throws Exception {
        insertEvent(801, "CLOSED", 2026);
        jdbcTemplate.update("insert into event_prize (event_id, prize_rank, name, quantity) values (801, 1, '상품', 2)");
        for (long userId = 1; userId <= 4; userId++) {
            insertEntry(userId);
            insertStamps(userId, 2);
        }
        // 스탬프가 모자란 응모는 후보에서 빠진다.
        insertEntry(5);
        insertStamps(5, 1);
        String url = BASE + "/events/801/draw";

        mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content("{\"confirm\":false}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content("{\"confirm\":true,\"seed\":\"s1\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventId").value(801))
                .andExpect(jsonPath("$.winnerCount").value(2));

        assertThat(jdbcTemplate.queryForObject("select count(*) from draw_entry where status = 'WON'", Long.class))
                .isEqualTo(2L);
        assertThat(jdbcTemplate.queryForObject(
                        "select count(*) from draw_entry where status = 'NOT_SELECTED'", Long.class))
                .isEqualTo(3L);
        assertThat(jdbcTemplate.queryForObject(
                        "select count(*) from draw_entry where status = 'ENTERED'", Long.class))
                .isZero();
        assertThat(jdbcTemplate.queryForObject(
                        "select count(*) from draw_entry where status = 'WON' and fulfillment_status = 'CONTACT_PENDING'"
                                + " and prize_id is not null",
                        Long.class))
                .isEqualTo(2L);

        mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content("{\"confirm\":true}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DRAW_ALREADY_EXECUTED"));

        // 추첨을 실행했으므로 추첨 완료로 바꿀 수 있다.
        mockMvc.perform(patch(BASE + "/events/801/status").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DRAWN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAWN"));
        mockMvc.perform(get(BASE + "/events/801/draw-result"))
                .andExpect(jsonPath("$.winners.length()").value(2));
    }

    @Test
    @WithMockUser(username = "9", roles = "OPERATOR")
    void drawRequiresClosedEvent() throws Exception {
        insertEvent(801, "OPEN", 2026);
        mockMvc.perform(post(BASE + "/events/801/draw").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"confirm\":true}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));
        mockMvc.perform(post(BASE + "/events/9999/draw").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"confirm\":true}"))
                .andExpect(status().isNotFound());
    }
}
