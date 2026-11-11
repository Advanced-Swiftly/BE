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
import java.sql.Timestamp;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
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
class StoreStatsControllerTest {

    private static final long EVENT_ID = 9101L;

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private StoreVerificationRepository verificationRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long storeId;

    @BeforeEach
    void setUp() {
        // @WithMockUser의 로그인 정보를 요청에 전달하려면 springSecurity()를 직접 적용해야 한다.
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        // 통계 집계만 검증하므로 사용자·이벤트 업체 FK 없이 방문 기록을 직접 넣는다.
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        jdbcTemplate.update("delete from visit_dog");
        jdbcTemplate.update("delete from visit");
        jdbcTemplate.update("delete from event_stamp");
        jdbcTemplate.update("delete from draw_entry");
        jdbcTemplate.update("delete from event_region where event_id = ?", EVENT_ID);
        jdbcTemplate.update("delete from stamp_event where event_id = ?", EVENT_ID);
        verificationRepository.deleteAll();
        storeRepository.deleteAll();

        storeId = saveManagedStore(1L);
        insertEvent();
        insertData();
    }

    @AfterEach
    void tearDown() {
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

    private void insertEvent() {
        jdbcTemplate.update(
                "insert into stamp_event (event_id, title, event_year, quarter, status, starts_on, ends_on,"
                        + " required_stamp_count, max_stamp_count) values (?, '통계 이벤트', 2026, 4, 'OPEN',"
                        + " '2026-10-01', '2026-12-31', 2, 10)",
                EVENT_ID);
    }

    private void visit(long userId, long forStoreId, String at) {
        jdbcTemplate.update(
                "insert into visit (user_id, store_id, qr_token_hash, visited_at) values (?, ?, 'hash', ?)",
                userId, forStoreId, Timestamp.from(Instant.parse(at)));
    }

    private void stamp(long userId, long forStoreId, String at) {
        jdbcTemplate.update(
                "insert into event_stamp (event_id, user_id, store_id, awarded_at) values (?, ?, ?, ?)",
                EVENT_ID, userId, forStoreId, Timestamp.from(Instant.parse(at)));
    }

    private void drawEntry(long userId) {
        jdbcTemplate.update(
                "insert into draw_entry (event_id, user_id, phone_number, address, privacy_agreed, status, created_at)"
                        + " values (?, ?, '010-0000-0000', '서울', true, 'ENTERED', ?)",
                EVENT_ID, userId, Timestamp.from(Instant.parse("2026-12-20T00:00:00Z")));
    }

    private void insertData() {
        long otherStoreId = storeId + 1000;
        // 이 업체의 방문: 이벤트 기간 안 5건(10/31, 11/2 두 건, 11/4, 11/9), 이벤트 기간 밖 1건(2027-01-02)
        visit(11, storeId, "2026-10-31T23:00:00Z");
        visit(11, storeId, "2026-11-02T01:00:00Z");
        visit(12, storeId, "2026-11-02T10:00:00Z");
        visit(13, storeId, "2026-11-04T05:00:00Z");
        visit(14, storeId, "2026-11-09T05:00:00Z");
        visit(15, storeId, "2027-01-02T00:00:00Z");
        // 다른 업체의 방문은 집계에 포함되지 않는다.
        visit(11, otherStoreId, "2026-11-03T00:00:00Z");

        stamp(11, storeId, "2026-11-03T00:00:00Z");
        stamp(12, storeId, "2026-11-05T00:00:00Z");
        stamp(13, storeId, "2026-12-15T00:00:00Z");
        stamp(11, otherStoreId, "2026-11-03T00:00:00Z");

        // 11, 13은 이 업체에서 스탬프를 받은 뒤 응모, 14는 이 업체의 스탬프 없이 응모
        drawEntry(11);
        drawEntry(13);
        drawEntry(14);
    }

    private String visitStatsUrl() {
        return "/api/v1/business/stores/" + storeId + "/visit-stats";
    }

    private String eventStatsUrl(Long forStoreId) {
        return "/api/v1/business/stores/" + forStoreId + "/event-stats";
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void aggregatesVisitsByDayByDefault() throws Exception {
        mockMvc.perform(get(visitStatsUrl()).param("from", "2026-11-01").param("to", "2026-11-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVisits").value(4))
                .andExpect(jsonPath("$.stampAwards").value(2))
                .andExpect(jsonPath("$.series.length()").value(3))
                .andExpect(jsonPath("$.series[0].date").value("2026-11-02"))
                .andExpect(jsonPath("$.series[0].visits").value(2))
                .andExpect(jsonPath("$.series[1].date").value("2026-11-04"))
                .andExpect(jsonPath("$.series[1].visits").value(1))
                .andExpect(jsonPath("$.series[2].date").value("2026-11-09"))
                .andExpect(jsonPath("$.series[2].visits").value(1));
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void aggregatesVisitsByWeek() throws Exception {
        mockMvc.perform(get(visitStatsUrl())
                        .param("from", "2026-11-01").param("to", "2026-11-30").param("groupBy", "WEEK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVisits").value(4))
                .andExpect(jsonPath("$.series.length()").value(2))
                .andExpect(jsonPath("$.series[0].date").value("2026-11-02"))
                .andExpect(jsonPath("$.series[0].visits").value(3))
                .andExpect(jsonPath("$.series[1].date").value("2026-11-09"))
                .andExpect(jsonPath("$.series[1].visits").value(1));
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void aggregatesVisitsByMonth() throws Exception {
        mockMvc.perform(get(visitStatsUrl())
                        .param("from", "2026-10-01").param("to", "2026-11-30").param("groupBy", "MONTH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVisits").value(5))
                .andExpect(jsonPath("$.series.length()").value(2))
                .andExpect(jsonPath("$.series[0].date").value("2026-10-01"))
                .andExpect(jsonPath("$.series[0].visits").value(1))
                .andExpect(jsonPath("$.series[1].date").value("2026-11-01"))
                .andExpect(jsonPath("$.series[1].visits").value(4));
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void includesVisitsOnTheLastDayOfRange() throws Exception {
        mockMvc.perform(get(visitStatsUrl()).param("from", "2026-11-04").param("to", "2026-11-04"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVisits").value(1));
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void returnsZeroWhenNoVisitsInRange() throws Exception {
        mockMvc.perform(get(visitStatsUrl()).param("from", "2025-01-01").param("to", "2025-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVisits").value(0))
                .andExpect(jsonPath("$.stampAwards").value(0))
                .andExpect(jsonPath("$.series").isEmpty());
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void rejectsInvalidVisitStatsParameters() throws Exception {
        // from, to 누락
        mockMvc.perform(get(visitStatsUrl()).param("to", "2026-11-30"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));
        mockMvc.perform(get(visitStatsUrl()).param("from", "2026-11-01"))
                .andExpect(status().isBadRequest());
        // 날짜 형식 오류
        mockMvc.perform(get(visitStatsUrl()).param("from", "20261101").param("to", "2026-11-30"))
                .andExpect(status().isBadRequest());
        // 종료일이 시작일보다 빠름
        mockMvc.perform(get(visitStatsUrl()).param("from", "2026-11-30").param("to", "2026-11-01"))
                .andExpect(status().isBadRequest());
        // 허용되지 않는 groupBy
        mockMvc.perform(get(visitStatsUrl())
                        .param("from", "2026-11-01").param("to", "2026-11-30").param("groupBy", "YEAR"))
                .andExpect(status().isBadRequest());
        // 기간이 366일을 넘음
        mockMvc.perform(get(visitStatsUrl()).param("from", "2025-01-01").param("to", "2026-11-30"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void cannotReadStatsOfStoreNotManagedByUser() throws Exception {
        Long othersStoreId = saveManagedStore(2L);

        mockMvc.perform(get("/api/v1/business/stores/" + othersStoreId + "/visit-stats")
                        .param("from", "2026-11-01").param("to", "2026-11-30"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_002"));
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void aggregatesEventStats() throws Exception {
        mockMvc.perform(get(eventStatsUrl(storeId)).param("eventId", String.valueOf(EVENT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storeId").value(storeId.intValue()))
                .andExpect(jsonPath("$.eventId").value((int) EVENT_ID))
                .andExpect(jsonPath("$.visits").value(5))
                .andExpect(jsonPath("$.stampAwards").value(3))
                .andExpect(jsonPath("$.drawConversions").value(2));
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void eventStatsRejectsMissingOrInvalidEventId() throws Exception {
        mockMvc.perform(get(eventStatsUrl(storeId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));
        mockMvc.perform(get(eventStatsUrl(storeId)).param("eventId", "abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void eventStatsForUnknownEventIsNotFound() throws Exception {
        mockMvc.perform(get(eventStatsUrl(storeId)).param("eventId", "999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "1", roles = "BUSINESS")
    void cannotReadEventStatsOfStoreNotManagedByUser() throws Exception {
        Long othersStoreId = saveManagedStore(2L);

        mockMvc.perform(get(eventStatsUrl(othersStoreId)).param("eventId", String.valueOf(EVENT_ID)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "1", roles = "GUARDIAN")
    void guardianIsForbidden() throws Exception {
        mockMvc.perform(get(visitStatsUrl()).param("from", "2026-11-01").param("to", "2026-11-30"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_002"));
    }

    @Test
    void anonymousUserIsUnauthorized() throws Exception {
        mockMvc.perform(get(visitStatsUrl()).param("from", "2026-11-01").param("to", "2026-11-30"))
                .andExpect(status().isUnauthorized());
    }
}
