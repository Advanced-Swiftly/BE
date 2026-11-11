package com.advancedswiftly.pawfit.business.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.advancedswiftly.pawfit.business.domain.ConditionRequest;
import com.advancedswiftly.pawfit.business.domain.PolicyRule;
import com.advancedswiftly.pawfit.business.domain.PolicyRuleType;
import com.advancedswiftly.pawfit.business.domain.QrStatus;
import com.advancedswiftly.pawfit.business.domain.Store;
import com.advancedswiftly.pawfit.business.domain.StoreQr;
import com.advancedswiftly.pawfit.business.domain.StoreVerification;
import com.advancedswiftly.pawfit.business.infrastructure.ConditionRequestRepository;
import com.advancedswiftly.pawfit.business.infrastructure.StoreBusinessHoursRepository;
import com.advancedswiftly.pawfit.business.infrastructure.StorePolicyRepository;
import com.advancedswiftly.pawfit.business.infrastructure.StoreQrRepository;
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
class StoreManagementControllerTest {

    private static final String BASE = "/api/v1/business";

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private StoreVerificationRepository verificationRepository;

    @Autowired
    private StorePolicyRepository policyRepository;

    @Autowired
    private ConditionRequestRepository requestRepository;

    @Autowired
    private StoreQrRepository qrRepository;

    @Autowired
    private StoreBusinessHoursRepository hoursRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        jdbcTemplate.update("delete from event_store");
        jdbcTemplate.update("delete from stamp_event");
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
        hoursRepository.deleteAll();
        qrRepository.deleteAll();
        requestRepository.deleteAll();
        policyRepository.deleteAll();
        verificationRepository.deleteAll();
        storeRepository.deleteAll();
    }

    @AfterEach
    void restoreIntegrity() {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
    }

    private Long saveManagedStore(Long userId) {
        Store store = storeRepository.save(Store.registerPending(
                "멍카페", "02-0000-0000", "서울특별시 성동구", new BigDecimal("37.5446"), new BigDecimal("127.0557")));
        StoreVerification verification = verificationRepository.save(StoreVerification.receive(
                store, userId, String.valueOf(1000000000L + store.getId()), "카페", "홍길동", "https://example.com/doc.pdf"));
        jdbcTemplate.update(
                "update store_verification set status = 'APPROVED' where verification_id = ?", verification.getId());
        return store.getId();
    }

    private Long saveRequest(Long userId, Long storeId, String status) {
        ConditionRequest saved = requestRepository.save(ConditionRequest.receive(
                storeId, 0, "정책 원문", userId, List.of(new PolicyRule(PolicyRuleType.EQUIPMENT, "CARRIER", true)), List.of()));
        jdbcTemplate.update("update condition_request set status = ? where request_id = ?", status, saved.getId());
        return saved.getId();
    }

    private void prepareEventSync(Long storeId) {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        jdbcTemplate.update("update store set region_id = 11 where store_id = ?", storeId);
        jdbcTemplate.update(
                "insert into stamp_event (event_id, title, event_year, quarter, status, starts_on, ends_on, "
                        + "required_stamp_count, max_stamp_count) "
                        + "values (801, '이벤트', 2026, 4, 'OPEN', '2026-10-01', '2026-12-31', 3, 5)");
        // region 행을 만들지 않으므로 이 테스트가 끝날 때까지 참조 무결성 검사를 끈다. (@AfterEach에서 복구)
    }

    // ---- 관리 업체 목록·상세

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void listsOnlyMyApprovedStores() throws Exception {
        Long mine = saveManagedStore(1L);
        saveManagedStore(2L);

        mockMvc.perform(get(BASE + "/stores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stores.length()").value(1))
                .andExpect(jsonPath("$.stores[0].storeId").value(mine))
                .andExpect(jsonPath("$.stores[0].name").value("멍카페"))
                .andExpect(jsonPath("$.stores[0].visibility").value("HIDDEN"));
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void returnsStoreDetailWithoutPolicyOrQr() throws Exception {
        Long storeId = saveManagedStore(1L);

        mockMvc.perform(get(BASE + "/stores/" + storeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storeId").value(storeId))
                .andExpect(jsonPath("$.address").value("서울특별시 성동구"))
                .andExpect(jsonPath("$.hours.length()").value(0))
                .andExpect(jsonPath("$.policyVersion").doesNotExist())
                .andExpect(jsonPath("$.qrStatus").doesNotExist());
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void rejectsDetailOfUnmanagedStore() throws Exception {
        Long storeId = saveManagedStore(2L);

        mockMvc.perform(get(BASE + "/stores/" + storeId)).andExpect(status().isForbidden());
    }

    // ---- 영업시간

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void updatesAndReadsBusinessHoursInWeekOrder() throws Exception {
        Long storeId = saveManagedStore(1L);

        mockMvc.perform(patch(BASE + "/stores/" + storeId + "/business-hours")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"hours": [
                                  {"dayOfWeek": "TUESDAY", "openTime": "10:00", "closeTime": "20:00"},
                                  {"dayOfWeek": "MONDAY", "closed": true}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storeId").value(storeId))
                .andExpect(jsonPath("$.updatedDays").value(2));

        // 같은 요일을 다시 보내면 값만 바뀐다.
        mockMvc.perform(patch(BASE + "/stores/" + storeId + "/business-hours")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"hours": [{"dayOfWeek": "TUESDAY", "openTime": "09:00", "closeTime": "18:30"}]}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get(BASE + "/stores/" + storeId + "/business-hours"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hours.length()").value(2))
                .andExpect(jsonPath("$.hours[0].dayOfWeek").value("MONDAY"))
                .andExpect(jsonPath("$.hours[0].closed").value(true))
                .andExpect(jsonPath("$.hours[0].openTime").doesNotExist())
                .andExpect(jsonPath("$.hours[1].dayOfWeek").value("TUESDAY"))
                .andExpect(jsonPath("$.hours[1].openTime").value("09:00"))
                .andExpect(jsonPath("$.hours[1].closeTime").value("18:30"));
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void rejectsInvalidBusinessHours() throws Exception {
        Long storeId = saveManagedStore(1L);
        String url = BASE + "/stores/" + storeId + "/business-hours";

        // 종료가 시작보다 빠름
        mockMvc.perform(patch(url).contentType(MediaType.APPLICATION_JSON).content("""
                        {"hours": [{"dayOfWeek": "MONDAY", "openTime": "20:00", "closeTime": "10:00"}]}"""))
                .andExpect(status().isBadRequest());
        // 영업일인데 시각 누락
        mockMvc.perform(patch(url).contentType(MediaType.APPLICATION_JSON).content("""
                        {"hours": [{"dayOfWeek": "MONDAY", "openTime": "10:00"}]}"""))
                .andExpect(status().isBadRequest());
        // 같은 요일 중복
        mockMvc.perform(patch(url).contentType(MediaType.APPLICATION_JSON).content("""
                        {"hours": [{"dayOfWeek": "MONDAY", "closed": true}, {"dayOfWeek": "MONDAY", "closed": true}]}"""))
                .andExpect(status().isBadRequest());
        // 빈 목록
        mockMvc.perform(patch(url).contentType(MediaType.APPLICATION_JSON).content("""
                        {"hours": []}"""))
                .andExpect(status().isBadRequest());
        assertThat(hoursRepository.count()).isZero();
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void rejectsBusinessHoursOfUnmanagedStore() throws Exception {
        Long storeId = saveManagedStore(2L);

        mockMvc.perform(get(BASE + "/stores/" + storeId + "/business-hours")).andExpect(status().isForbidden());
    }

    // ---- 입장 조건 요청 목록

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void listsConditionRequestsLatestFirstWithStatusFilter() throws Exception {
        Long storeId = saveManagedStore(1L);
        Long first = saveRequest(1L, storeId, "RECEIVED");
        Long second = saveRequest(1L, storeId, "APPROVED");
        String url = BASE + "/stores/" + storeId + "/condition-requests";

        mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].requestId").value(second))
                .andExpect(jsonPath("$.items[0].kind").value("CONDITION_REQUEST"))
                .andExpect(jsonPath("$.items[1].requestId").value(first));
        mockMvc.perform(get(url).param("status", "APPROVED"))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].status").value("APPROVED"));
        mockMvc.perform(get(url).param("page", "2")).andExpect(jsonPath("$.items.length()").value(0));
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void rejectsBadListParams() throws Exception {
        Long storeId = saveManagedStore(1L);
        String url = BASE + "/stores/" + storeId + "/condition-requests";

        mockMvc.perform(get(url).param("status", "NOPE")).andExpect(status().isBadRequest());
        mockMvc.perform(get(url).param("page", "0")).andExpect(status().isBadRequest());
        mockMvc.perform(get(url).param("page", "abc")).andExpect(status().isBadRequest());
    }

    // ---- 입장 조건 요청 취소·보완

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void cancelsReceivedRequest() throws Exception {
        Long storeId = saveManagedStore(1L);
        Long requestId = saveRequest(1L, storeId, "RECEIVED");

        mockMvc.perform(patch(BASE + "/condition-requests/" + requestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\": \"CANCEL\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value(requestId))
                .andExpect(jsonPath("$.status").value("CANCELED"));
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void rejectsCancelOfFinishedRequest() throws Exception {
        Long storeId = saveManagedStore(1L);
        Long requestId = saveRequest(1L, storeId, "APPROVED");

        mockMvc.perform(patch(BASE + "/condition-requests/" + requestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\": \"CANCEL\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST_STATE"));
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void resubmitsSupplementRequestAndAppendsEvidence() throws Exception {
        Long storeId = saveManagedStore(1L);
        Long requestId = saveRequest(1L, storeId, "NEEDS_SUPPLEMENT");

        mockMvc.perform(patch(BASE + "/condition-requests/" + requestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"action": "RESUBMIT",
                                 "sourceText": "보완한 원문",
                                 "evidenceUrls": ["https://example.com/more.png"]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECEIVED"));

        assertThat(jdbcTemplate.queryForObject(
                        "select source_text from condition_request where request_id = ?", String.class, requestId))
                .isEqualTo("보완한 원문");
        assertThat(jdbcTemplate.queryForList(
                        "select evidence_url from condition_request_evidence where request_id = ?",
                        String.class, requestId))
                .containsExactly("https://example.com/more.png");
        // 정책 변경안을 생략했으므로 기존 규칙이 유지된다.
        assertThat(jdbcTemplate.queryForObject(
                        "select count(*) from condition_request_rule where request_id = ?", Long.class, requestId))
                .isEqualTo(1L);
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void rejectsResubmitWithoutSupplementRequest() throws Exception {
        Long storeId = saveManagedStore(1L);
        Long requestId = saveRequest(1L, storeId, "RECEIVED");

        mockMvc.perform(patch(BASE + "/condition-requests/" + requestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\": \"RESUBMIT\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST_STATE"));
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void hidesOtherUsersRequest() throws Exception {
        Long storeId = saveManagedStore(2L);
        Long requestId = saveRequest(2L, storeId, "RECEIVED");

        mockMvc.perform(patch(BASE + "/condition-requests/" + requestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\": \"CANCEL\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void rejectsMissingOrUnknownAction() throws Exception {
        Long storeId = saveManagedStore(1L);
        Long requestId = saveRequest(1L, storeId, "RECEIVED");

        mockMvc.perform(patch(BASE + "/condition-requests/" + requestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch(BASE + "/condition-requests/" + requestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\": \"DELETE\"}"))
                .andExpect(status().isBadRequest());
    }

    // ---- QR 발급·재발급

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void issuesFirstQrWithoutBody() throws Exception {
        Long storeId = saveManagedStore(1L);

        mockMvc.perform(post(BASE + "/stores/" + storeId + "/qr"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.qrId").isNumber())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.revokedPreviousQr").value(false));

        StoreQr saved = qrRepository.findAll().get(0);
        assertThat(saved.getQrTokenHash()).hasSize(64);
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void reissueRevokesPreviousQrAndBlocksReactivation() throws Exception {
        Long storeId = saveManagedStore(1L);
        mockMvc.perform(post(BASE + "/stores/" + storeId + "/qr")).andExpect(status().isCreated());
        Long firstId = qrRepository.findAll().get(0).getId();

        mockMvc.perform(post(BASE + "/stores/" + storeId + "/qr")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\": \"QR 코드 분실\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.revokedPreviousQr").value(true));

        assertThat(qrRepository.findById(firstId).orElseThrow().getStatus()).isEqualTo(QrStatus.REVOKED);
        StoreQr current = qrRepository.findFirstByStoreIdOrderByIdDesc(storeId).orElseThrow();
        assertThat(current.getStatus()).isEqualTo(QrStatus.ACTIVE);
        assertThat(current.getIssueReason()).isEqualTo("QR 코드 분실");
        assertThat(current.getQrTokenHash()).isNotEqualTo(qrRepository.findById(firstId).orElseThrow().getQrTokenHash());

    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void revokedQrCannotBeReactivated() throws Exception {
        Long storeId = saveManagedStore(1L);
        StoreQr qr = qrRepository.save(StoreQr.issue(storeId, "https://example.com/qr.png"));
        jdbcTemplate.update("update store_qr set status = 'REVOKED' where qr_id = ?", qr.getId());

        mockMvc.perform(patch(BASE + "/stores/" + storeId + "/qr")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\": true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST_STATE"));
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void rejectsTooLongReasonAndUnmanagedStore() throws Exception {
        Long storeId = saveManagedStore(1L);
        Long other = saveManagedStore(2L);

        mockMvc.perform(post(BASE + "/stores/" + storeId + "/qr")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\": \"" + "가".repeat(201) + "\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(BASE + "/stores/" + other + "/qr")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void issuingQrCreatesEventStoreWithSameIdAndToggleFollows() throws Exception {
        Long storeId = saveManagedStore(1L);
        prepareEventSync(storeId);

        mockMvc.perform(post(BASE + "/stores/" + storeId + "/qr")).andExpect(status().isCreated());

        StoreQr qr = qrRepository.findFirstByStoreIdOrderByIdDesc(storeId).orElseThrow();
        assertThat(jdbcTemplate.queryForObject(
                        "select qr_token_hash from event_store where store_id = ?", String.class, storeId))
                .isEqualTo(qr.getQrTokenHash());
        assertThat(jdbcTemplate.queryForObject(
                        "select qr_revoked from event_store where store_id = ?", Boolean.class, storeId))
                .isFalse();

        mockMvc.perform(patch(BASE + "/stores/" + storeId + "/qr")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\": false}"))
                .andExpect(status().isOk());
        assertThat(jdbcTemplate.queryForObject(
                        "select qr_revoked from event_store where store_id = ?", Boolean.class, storeId))
                .isTrue();

        // 재발급하면 같은 행의 해시가 새 QR로 바뀌고 다시 사용 가능해진다.
        mockMvc.perform(post(BASE + "/stores/" + storeId + "/qr")).andExpect(status().isCreated());
        StoreQr reissued = qrRepository.findFirstByStoreIdOrderByIdDesc(storeId).orElseThrow();
        assertThat(jdbcTemplate.queryForObject("select count(*) from event_store", Long.class)).isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject(
                        "select qr_token_hash from event_store where store_id = ?", String.class, storeId))
                .isEqualTo(reissued.getQrTokenHash());
        assertThat(jdbcTemplate.queryForObject(
                        "select qr_revoked from event_store where store_id = ?", Boolean.class, storeId))
                .isFalse();
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void skipsEventStoreSyncWithoutRegion() throws Exception {
        Long storeId = saveManagedStore(1L);

        mockMvc.perform(post(BASE + "/stores/" + storeId + "/qr")).andExpect(status().isCreated());

        assertThat(jdbcTemplate.queryForObject("select count(*) from event_store", Long.class)).isZero();
    }
}
