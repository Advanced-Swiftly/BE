package com.advancedswiftly.pawfit.event.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.advancedswiftly.pawfit.auth.domain.User;
import com.advancedswiftly.pawfit.auth.domain.UserRole;
import com.advancedswiftly.pawfit.auth.infrastructure.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class EventControllerIntegrationTest {
    private static final String QR_100 = "PAWFIT-DEMO-QR-100";
    private static final String QR_101 = "PAWFIT-DEMO-QR-101";

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;
    private User owner;
    private User otherGuardian;
    private User businessUser;

    @BeforeEach
    void setUp() {
        cleanEventData();
        jdbcTemplate.update("delete from app_user");
        jdbcTemplate.update("delete from region");
        jdbcTemplate.update("insert into region (region_id, name, active) values (11, '서울특별시', true)");
        owner = userRepository.saveAndFlush(User.register(
                "01011112222", passwordEncoder.encode("password123!"), "보호자", UserRole.GUARDIAN));
        otherGuardian = userRepository.saveAndFlush(User.register(
                "01033334444", passwordEncoder.encode("password123!"), "다른 보호자", UserRole.GUARDIAN));
        businessUser = userRepository.saveAndFlush(User.register(
                "01055556666", passwordEncoder.encode("password123!"), "업체", UserRole.BUSINESS));
        seedEvent();
    }

    @AfterEach
    void tearDown() { cleanEventData(); }

    @Test
    void guardianCanReadCurrentEventDetailAndDistanceSortedStores() throws Exception {
        mockMvc.perform(get("/api/v1/events/current").with(guardian(owner)).param("regionId", "11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value(801))
                .andExpect(jsonPath("$.status").value("OPEN"));

        mockMvc.perform(get("/api/v1/events/{eventId}", 801).with(guardian(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.regions[0]").value(11))
                .andExpect(jsonPath("$.prizes[0].name").value("펫 유모차"));

        mockMvc.perform(get("/api/v1/events/{eventId}/stores", 801)
                        .with(guardian(owner)).param("latitude", "37.5665").param("longitude", "126.9780"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].storeId").value(100))
                .andExpect(jsonPath("$.items[0].distanceMeters").value(0));
    }

    @Test
    void qrVisitsAwardUniqueStampsAndEnableDrawEntry() throws Exception {
        visit(owner, QR_100).andExpect(status().isCreated())
                .andExpect(jsonPath("$.stampAwarded").value(true))
                .andExpect(jsonPath("$.stampCount").value(1));

        visit(owner, QR_100).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_OR_REVOKED_QR"));

        visit(owner, QR_101).andExpect(status().isCreated())
                .andExpect(jsonPath("$.stampCount").value(2));

        mockMvc.perform(get("/api/v1/visits").with(guardian(owner)).param("page", "0"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(2));

        mockMvc.perform(get("/api/v1/events/{eventId}/stamps", 801).with(guardian(owner)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.stampCount").value(2))
                .andExpect(jsonPath("$.maxStampCount").value(10));

        String entryBody = """
                {"phoneNumber":"01012345678","address":"서울특별시 중구 1","privacyAgreed":true}
                """;
        mockMvc.perform(post("/api/v1/events/{eventId}/draw-entries", 801)
                        .with(guardian(owner)).contentType(MediaType.APPLICATION_JSON).content(entryBody))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("ENTERED"))
                .andExpect(jsonPath("$.stampCount").value(2));

        mockMvc.perform(get("/api/v1/events/{eventId}/draw-entry", 801).with(guardian(owner)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ENTERED"))
                .andExpect(jsonPath("$.prize").doesNotExist());

        mockMvc.perform(post("/api/v1/events/{eventId}/draw-entries", 801)
                        .with(guardian(owner)).contentType(MediaType.APPLICATION_JSON).content(entryBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DRAW_ENTRY_ALREADY_EXISTS"));
    }

    @Test
    void insufficientStampsAndInvalidFiltersAreRejected() throws Exception {
        visit(otherGuardian, QR_100).andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/events/{eventId}/draw-entries", 801)
                        .with(guardian(otherGuardian)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"01012345678\",\"address\":\"서울\",\"privacyAgreed\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_STAMPS"));

        mockMvc.perform(get("/api/v1/visits").with(guardian(owner))
                        .param("from", "2026-12-01").param("to", "2026-11-01"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_001"));

        mockMvc.perform(get("/api/v1/events/801/stores").with(guardian(owner)).param("latitude", "37.5"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_001"));
    }

    @Test
    void nonGuardianAndAnonymousRequestsAreRejected() throws Exception {
        mockMvc.perform(get("/api/v1/events/current")
                        .with(user(businessUser.getId().toString()).roles("BUSINESS")))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("AUTH_002"));
        mockMvc.perform(get("/api/v1/events/current"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("AUTH_001"));
    }

    private org.springframework.test.web.servlet.ResultActions visit(User user, String token) throws Exception {
        return mockMvc.perform(post("/api/v1/visits/qr").with(guardian(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"qrToken\":\"" + token + "\",\"dogIds\":[]}"));
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor guardian(User user) {
        return org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                .user(user.getId().toString()).roles("GUARDIAN");
    }

    private void seedEvent() {
        jdbcTemplate.update("""
                insert into stamp_event (event_id,title,event_year,quarter,status,starts_on,ends_on,
                required_stamp_count,max_stamp_count,notice)
                values (801,'2026 4분기 스탬프로드',2026,4,'OPEN','2026-10-01','2026-12-31',2,10,'유의사항')
                """);
        jdbcTemplate.update("insert into event_region (event_id,region_id) values (801,11)");
        jdbcTemplate.update("insert into event_prize (event_id,prize_rank,name,quantity) values (801,1,'펫 유모차',1)");
        jdbcTemplate.update("""
                insert into event_store (store_id,event_id,region_id,name,latitude,longitude,approved,
                qr_token_hash,qr_expires_at,qr_revoked) values
                (100,801,11,'멍카페',37.5665,126.9780,true,
                '0834d5f0fb318c168a112e3f961fc3a8378c8d171d2435e8b2152e7c3cc93ec6','2027-01-01T00:00:00Z',false),
                (101,801,11,'댕댕마켓',37.5651,126.98955,true,
                '9c4d4ba0b0bb32b3f1e4f6014081c7dae328d5fc7c8d5a279733d70ffe779355','2027-01-01T00:00:00Z',false)
                """);
    }

    private void cleanEventData() {
        jdbcTemplate.update("delete from draw_entry");
        jdbcTemplate.update("delete from event_stamp");
        jdbcTemplate.update("delete from visit_dog");
        jdbcTemplate.update("delete from visit");
        jdbcTemplate.update("delete from event_store");
        jdbcTemplate.update("delete from event_prize");
        jdbcTemplate.update("delete from event_region");
        jdbcTemplate.update("delete from stamp_event");
    }
}
