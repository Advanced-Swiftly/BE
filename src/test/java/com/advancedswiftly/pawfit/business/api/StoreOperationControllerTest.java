package com.advancedswiftly.pawfit.business.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.advancedswiftly.pawfit.business.domain.PolicyRule;
import com.advancedswiftly.pawfit.business.domain.PolicyRuleType;
import com.advancedswiftly.pawfit.business.domain.QrStatus;
import com.advancedswiftly.pawfit.business.domain.Store;
import com.advancedswiftly.pawfit.business.domain.StorePolicy;
import com.advancedswiftly.pawfit.business.domain.StoreQr;
import com.advancedswiftly.pawfit.business.domain.StoreVerification;
import com.advancedswiftly.pawfit.business.infrastructure.StorePolicyRepository;
import com.advancedswiftly.pawfit.business.infrastructure.StoreQrRepository;
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
class StoreOperationControllerTest {

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
    private StoreQrRepository qrRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        // @WithMockUser의 로그인 정보를 요청에 전달하려면 springSecurity()를 직접 적용해야 한다.
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        qrRepository.deleteAll();
        policyRepository.deleteAll();
        verificationRepository.deleteAll();
        storeRepository.deleteAll();
    }

    private String storeUrl(Long storeId) {
        return "/api/v1/business/stores/" + storeId;
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

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void updatesOnlyProvidedFields() throws Exception {
        Long storeId = saveManagedStore(1L);

        mockMvc.perform(patch(storeUrl(storeId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"새 멍카페\", \"description\": \"반려견 동반 카페\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storeId").value(storeId.intValue()))
                .andExpect(jsonPath("$.name").value("새 멍카페"));

        Store store = storeRepository.findById(storeId).orElseThrow();
        assertThat(store.getName()).isEqualTo("새 멍카페");
        assertThat(store.getDescription()).isEqualTo("반려견 동반 카페");
        // 전달하지 않은 연락처는 기존 값을 유지한다.
        assertThat(store.getPhone()).isEqualTo("02-0000-0000");
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void updatesPhoneNumber() throws Exception {
        Long storeId = saveManagedStore(1L);

        mockMvc.perform(patch(storeUrl(storeId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\": \"02-9999-8888\"}"))
                .andExpect(status().isOk());

        assertThat(storeRepository.findById(storeId).orElseThrow().getPhone()).isEqualTo("02-9999-8888");
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void replacesAndClearsAmenities() throws Exception {
        Long storeId = saveManagedStore(1L);

        mockMvc.perform(patch(storeUrl(storeId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amenities\": [\"PARKING\", \"TERRACE\"]}"))
                .andExpect(status().isOk());
        assertThat(amenitiesOf(storeId)).containsExactlyInAnyOrder("PARKING", "TERRACE");

        mockMvc.perform(patch(storeUrl(storeId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amenities\": [\"WIFI\"]}"))
                .andExpect(status().isOk());
        assertThat(amenitiesOf(storeId)).containsExactly("WIFI");

        // 생략하면 유지, 빈 배열이면 모두 제거한다.
        mockMvc.perform(patch(storeUrl(storeId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"멍카페\"}"))
                .andExpect(status().isOk());
        assertThat(amenitiesOf(storeId)).containsExactly("WIFI");

        mockMvc.perform(patch(storeUrl(storeId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amenities\": []}"))
                .andExpect(status().isOk());
        assertThat(amenitiesOf(storeId)).isEmpty();
    }

    private List<String> amenitiesOf(Long storeId) {
        return jdbcTemplate.queryForList(
                "select amenity_code from store_amenity where store_id = ?", String.class, storeId);
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void rejectsBlankNameAndTooLongPhone() throws Exception {
        Long storeId = saveManagedStore(1L);

        mockMvc.perform(patch(storeUrl(storeId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));

        mockMvc.perform(patch(storeUrl(storeId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\": \"123456789012345678901\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void cannotUpdateStoreNotManagedByUser() throws Exception {
        Long othersStoreId = saveManagedStore(2L);

        mockMvc.perform(patch(storeUrl(othersStoreId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"가로채기\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_002"));
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void returnsCurrentPolicyWithHighestVersion() throws Exception {
        Long storeId = saveManagedStore(1L);
        policyRepository.save(StorePolicy.create(storeId, 2, "이전 정책", List.of()));
        policyRepository.save(StorePolicy.create(storeId, 3, "현재 정책", List.of(
                new PolicyRule(PolicyRuleType.EQUIPMENT, "CARRIER", true),
                new PolicyRule(PolicyRuleType.BREED, "PITBULL", true),
                new PolicyRule(PolicyRuleType.VACCINE, "RABIES", false))));

        mockMvc.perform(get(storeUrl(storeId) + "/policy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.policyId").isNumber())
                .andExpect(jsonPath("$.version").value(3))
                .andExpect(jsonPath("$.equipmentRules.length()").value(1))
                .andExpect(jsonPath("$.equipmentRules[0].code").value("CARRIER"))
                .andExpect(jsonPath("$.equipmentRules[0].required").value(true))
                .andExpect(jsonPath("$.breedRules[0].code").value("PITBULL"))
                .andExpect(jsonPath("$.vaccineRules[0].code").value("RABIES"))
                .andExpect(jsonPath("$.vaccineRules[0].required").value(false));
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void policyIsNotFoundWhenNoneExists() throws Exception {
        Long storeId = saveManagedStore(1L);

        mockMvc.perform(get(storeUrl(storeId) + "/policy"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMON_002"));
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void cannotReadPolicyOfStoreNotManagedByUser() throws Exception {
        Long othersStoreId = saveManagedStore(2L);

        mockMvc.perform(get(storeUrl(othersStoreId) + "/policy"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void returnsLatestQr() throws Exception {
        Long storeId = saveManagedStore(1L);
        qrRepository.save(StoreQr.issue(storeId, "https://cdn.pawfit.app/qr/old.png"));
        StoreQr latest = qrRepository.save(StoreQr.issue(storeId, "https://cdn.pawfit.app/qr/new.png"));

        mockMvc.perform(get(storeUrl(storeId) + "/qr"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.qrId").value(latest.getId().intValue()))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.qrImageUrl").value("https://cdn.pawfit.app/qr/new.png"));
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void qrIsNotFoundWhenNotIssued() throws Exception {
        Long storeId = saveManagedStore(1L);

        mockMvc.perform(get(storeUrl(storeId) + "/qr"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void disablesAndReactivatesQr() throws Exception {
        Long storeId = saveManagedStore(1L);
        StoreQr qr = qrRepository.save(StoreQr.issue(storeId, "https://cdn.pawfit.app/qr/1.png"));

        mockMvc.perform(patch(storeUrl(storeId) + "/qr")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\": false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.qrId").value(qr.getId().intValue()))
                .andExpect(jsonPath("$.status").value("DISABLED"))
                .andExpect(jsonPath("$.qrImageUrl").doesNotExist());
        assertThat(qrRepository.findById(qr.getId()).orElseThrow().getStatus()).isEqualTo(QrStatus.DISABLED);

        mockMvc.perform(patch(storeUrl(storeId) + "/qr")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\": true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void qrActiveIsRequired() throws Exception {
        Long storeId = saveManagedStore(1L);
        qrRepository.save(StoreQr.issue(storeId, "https://cdn.pawfit.app/qr/1.png"));

        mockMvc.perform(patch(storeUrl(storeId) + "/qr")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void cannotChangeQrOfStoreNotManagedByUser() throws Exception {
        Long othersStoreId = saveManagedStore(2L);
        qrRepository.save(StoreQr.issue(othersStoreId, "https://cdn.pawfit.app/qr/1.png"));

        mockMvc.perform(patch(storeUrl(othersStoreId) + "/qr")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\": false}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "1", roles = "GUARDIAN")
    void guardianIsForbidden() throws Exception {
        mockMvc.perform(get(storeUrl(1L) + "/policy"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_002"));
    }

    @Test
    void anonymousUserIsUnauthorized() throws Exception {
        mockMvc.perform(get(storeUrl(1L) + "/qr"))
                .andExpect(status().isUnauthorized());
    }
}
