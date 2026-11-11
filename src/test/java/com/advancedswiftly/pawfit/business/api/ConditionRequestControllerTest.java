package com.advancedswiftly.pawfit.business.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.advancedswiftly.pawfit.business.domain.ConditionRequest;
import com.advancedswiftly.pawfit.business.domain.ConditionRequestStatus;
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
class ConditionRequestControllerTest {

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
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        // @WithMockUser의 로그인 정보를 요청에 전달하려면 springSecurity()를 직접 적용해야 한다.
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        requestRepository.deleteAll();
        policyRepository.deleteAll();
        verificationRepository.deleteAll();
        storeRepository.deleteAll();
    }

    private String createUrl(Long storeId) {
        return "/api/v1/business/stores/" + storeId + "/condition-requests";
    }

    private String detailUrl(Long requestId) {
        return "/api/v1/business/condition-requests/" + requestId;
    }

    /** userId가 관리하는(인증이 승인된) 업체를 만든다. */
    private Long saveManagedStore(Long userId) {
        Store store = storeRepository.save(Store.registerPending(
                "멍카페", "02-0000-0000", "서울특별시 성동구", new BigDecimal("37.5446"), new BigDecimal("127.0557")));
        StoreVerification verification = verificationRepository.save(StoreVerification.receive(
                store, userId, String.valueOf(1000000000L + store.getId()), "카페", "홍길동", "https://example.com/doc.pdf"));
        jdbcTemplate.update(
                "update store_verification set status = 'APPROVED' where verification_id = ?", verification.getId());
        return store.getId();
    }

    private ConditionRequest saveRequest(Long userId, Long storeId) {
        return requestRepository.save(ConditionRequest.receive(
                storeId, 0, "정책 원문", userId, List.of(new PolicyRule(PolicyRuleType.EQUIPMENT, "CARRIER", true)), List.of()));
    }

    private String body(int baseVersion) {
        return """
                {"baseVersion": %d,
                 "requestedPolicy": {
                    "breedRules": [{"code": "PITBULL", "required": true}],
                    "equipmentRules": [{"code": "CARRIER", "required": true}],
                    "vaccineRules": []},
                 "sourceText": "소형견만 입장 가능하며 이동장이 필요합니다.",
                 "evidenceUrls": ["https://example.com/evidence.png"]}
                """.formatted(baseVersion);
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void createsRequestWhenNoPolicyYet() throws Exception {
        Long storeId = saveManagedStore(1L);

        mockMvc.perform(post(createUrl(storeId)).contentType(MediaType.APPLICATION_JSON).content(body(0)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.requestId").isNumber())
                .andExpect(jsonPath("$.status").value("RECEIVED"));

        ConditionRequest saved = requestRepository.findAll().get(0);
        assertThat(saved.getStatus()).isEqualTo(ConditionRequestStatus.RECEIVED);
        assertThat(saved.getBaseVersion()).isZero();
        assertThat(saved.getSubmittedBy()).isEqualTo(1L);
        // 지연 로딩 컬렉션은 트랜잭션 밖에서 읽을 수 없으므로 SQL로 확인한다.
        assertThat(jdbcTemplate.queryForObject(
                        "select count(*) from condition_request_rule where request_id = ?", Long.class, saved.getId()))
                .isEqualTo(2L);
        assertThat(jdbcTemplate.queryForList(
                        "select evidence_url from condition_request_evidence where request_id = ?",
                        String.class, saved.getId()))
                .containsExactly("https://example.com/evidence.png");
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void createsRequestWhenBaseVersionMatchesCurrent() throws Exception {
        Long storeId = saveManagedStore(1L);
        policyRepository.save(StorePolicy.create(storeId, 3, "현재 정책", List.of()));

        mockMvc.perform(post(createUrl(storeId)).contentType(MediaType.APPLICATION_JSON).content(body(3)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void conflictsWhenBaseVersionIsStale() throws Exception {
        Long storeId = saveManagedStore(1L);
        policyRepository.save(StorePolicy.create(storeId, 3, "현재 정책", List.of()));

        mockMvc.perform(post(createUrl(storeId)).contentType(MediaType.APPLICATION_JSON).content(body(2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("POLICY_VERSION_CONFLICT"));

        assertThat(requestRepository.count()).isZero();
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void conflictsWhenBaseVersionGivenButNoPolicyExists() throws Exception {
        Long storeId = saveManagedStore(1L);

        mockMvc.perform(post(createUrl(storeId)).contentType(MediaType.APPLICATION_JSON).content(body(1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("POLICY_VERSION_CONFLICT"));
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void acceptsRequestWithoutEvidenceAndWithPartialPolicy() throws Exception {
        Long storeId = saveManagedStore(1L);

        mockMvc.perform(post(createUrl(storeId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"baseVersion": 0, "requestedPolicy": {}, "sourceText": "제한 없음"}
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void rejectsInvalidRequestBody() throws Exception {
        Long storeId = saveManagedStore(1L);

        // sourceText 누락
        mockMvc.perform(post(createUrl(storeId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"baseVersion\": 0, \"requestedPolicy\": {}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));

        // requestedPolicy 누락
        mockMvc.perform(post(createUrl(storeId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"baseVersion\": 0, \"sourceText\": \"원문\"}"))
                .andExpect(status().isBadRequest());

        // 근거 URL 형식 오류
        mockMvc.perform(post(createUrl(storeId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"baseVersion": 0, "requestedPolicy": {}, "sourceText": "원문",
                                 "evidenceUrls": ["not-a-url"]}
                                """))
                .andExpect(status().isBadRequest());

        // 조건 코드 누락
        mockMvc.perform(post(createUrl(storeId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"baseVersion": 0, "sourceText": "원문",
                                 "requestedPolicy": {"breedRules": [{"required": true}]}}
                                """))
                .andExpect(status().isBadRequest());

        // 음수 기준 버전
        mockMvc.perform(post(createUrl(storeId)).contentType(MediaType.APPLICATION_JSON).content(body(-1)))
                .andExpect(status().isBadRequest());

        assertThat(requestRepository.count()).isZero();
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void cannotRequestForStoreNotManagedByUser() throws Exception {
        Long othersStoreId = saveManagedStore(2L);

        mockMvc.perform(post(createUrl(othersStoreId)).contentType(MediaType.APPLICATION_JSON).content(body(0)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_002"));
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void returnsDetailWithoutCommentBeforeReview() throws Exception {
        ConditionRequest request = saveRequest(1L, saveManagedStore(1L));

        mockMvc.perform(get(detailUrl(request.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value(request.getId().intValue()))
                .andExpect(jsonPath("$.status").value("RECEIVED"))
                .andExpect(jsonPath("$.reviewComment").doesNotExist());
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void returnsReviewCommentInDetail() throws Exception {
        ConditionRequest request = saveRequest(1L, saveManagedStore(1L));
        jdbcTemplate.update(
                "update condition_request set status = 'NEEDS_SUPPLEMENT', review_note = ? where request_id = ?",
                "예방접종 기준을 명시해 주세요.", request.getId());

        mockMvc.perform(get(detailUrl(request.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NEEDS_SUPPLEMENT"))
                .andExpect(jsonPath("$.reviewComment").value("예방접종 기준을 명시해 주세요."));
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void othersRequestDetailIsNotFound() throws Exception {
        ConditionRequest request = saveRequest(2L, saveManagedStore(2L));

        mockMvc.perform(get(detailUrl(request.getId())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMON_002"));
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void unknownRequestIsNotFound() throws Exception {
        mockMvc.perform(get(detailUrl(999999L)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "1", roles = "GUARDIAN")
    void guardianIsForbidden() throws Exception {
        mockMvc.perform(get(detailUrl(1L)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_002"));
    }

    @Test
    void anonymousUserIsUnauthorized() throws Exception {
        mockMvc.perform(get(detailUrl(1L)))
                .andExpect(status().isUnauthorized());
    }
}
