package com.advancedswiftly.pawfit.place;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.advancedswiftly.pawfit.auth.domain.*;
import com.advancedswiftly.pawfit.auth.infrastructure.*;
import com.advancedswiftly.pawfit.business.domain.*;
import com.advancedswiftly.pawfit.business.infrastructure.*;
import com.advancedswiftly.pawfit.dog.domain.Dog;
import com.advancedswiftly.pawfit.dog.infrastructure.DogRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:scenario;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
            "spring.jpa.hibernate.ddl-auto=create-drop"
        })
class ScenarioPolicyIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate db;
    @Autowired UserRepository users;
    @Autowired BreedRepository breeds;
    @Autowired DogRepository dogs;
    @Autowired StoreRepository stores;
    @Autowired StorePolicyRepository policies;
    @Autowired ConditionRequestRepository requests;

    private EntryConditions conditions(int weight, boolean outdoor, java.time.LocalDate effective) {
        return new EntryConditions(
                true,
                new EntryConditions.Area(true, BigDecimal.valueOf(weight), List.of("STROLLER")),
                new EntryConditions.Area(outdoor, null, List.of("LEASH")),
                effective,
                "실내·야외 정책",
                "조건 변경",
                false);
    }

    private void migration(String name) throws Exception {
        for (String sql :
                new ClassPathResource("db/migration/" + name)
                        .getContentAsString(StandardCharsets.UTF_8)
                        .split(";"))
            if (!sql.isBlank() && !sql.contains("ENABLE ROW LEVEL SECURITY"))
                db.execute(
                        sql.replace("ADD COLUMN region_id", "ADD COLUMN IF NOT EXISTS region_id"));
    }

    @Test
    void approvalAndEffectiveDateDriveBothAppsAndTogetherUsesTheSameArea() throws Exception {
        migration("V8__create_vaccination_tables.sql");
        migration("V9__create_place_catalog.sql");
        var guardian =
                users.saveAndFlush(User.register("01099000001", "hash", "보호자", UserRole.GUARDIAN));
        var breed = breeds.saveAndFlush(Breed.create("말티즈"));
        var mong =
                dogs.saveAndFlush(
                        Dog.create(
                                guardian,
                                breed,
                                "몽이",
                                null,
                                BigDecimal.valueOf(5),
                                true,
                                List.of("STROLLER", "LEASH")));
        var kong =
                dogs.saveAndFlush(
                        Dog.create(
                                guardian,
                                breed,
                                "콩이",
                                null,
                                BigDecimal.valueOf(8),
                                false,
                                List.of("LEASH")));
        var strollerDog =
                dogs.saveAndFlush(
                        Dog.create(
                                guardian,
                                breed,
                                "유모차견",
                                null,
                                BigDecimal.valueOf(8),
                                false,
                                List.of("STROLLER", "LEASH")));
        var store =
                stores.saveAndFlush(
                        Store.registerPending(
                                "온유커피 성수",
                                "02-0000-0000",
                                "서울",
                                BigDecimal.valueOf(37.5446),
                                BigDecimal.valueOf(127.0557)));
        db.update(
                "update store set verification_status='VERIFIED',category='CAFE' where store_id=?",
                store.getId());
        var current = StorePolicy.create(store.getId(), 1, "실내 7kg", List.of());
        current.setConditions(conditions(7, false, EntryConditions.today().minusDays(1)));
        policies.saveAndFlush(current);
        var auth = user(guardian.getId().toString()).roles("GUARDIAN");
        String url = "/api/v1/places/STORE:" + store.getId() + "/entry-judgments";
        String body = "{\"dogIds\":[" + strollerDog.getId() + "]}";
        mvc.perform(post(url).with(auth).contentType("application/json").content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overallVerdict").value("ENTRY_DENIED"));
        var requested =
                ConditionRequest.receive(store.getId(), 1, "실내 10kg", 99L, List.of(), List.of());
        requested.setConditions(conditions(10, false, EntryConditions.today()));
        requested.review(ConditionRequestStatus.IN_REVIEW, null, 999L);
        requests.saveAndFlush(requested);
        mvc.perform(post(url).with(auth).contentType("application/json").content(body))
                .andExpect(jsonPath("$.overallVerdict").value("ENTRY_DENIED"));
        mvc.perform(
                        patch("/api/v1/admin/condition-requests/" + requested.getId())
                                .with(user("999").roles("OPERATOR"))
                                .contentType("application/json")
                                .content("{\"action\":\"APPROVE\"}"))
                .andExpect(status().isOk());
        mvc.perform(post(url).with(auth).contentType("application/json").content(body))
                .andExpect(jsonPath("$.overallVerdict").value("ENTRY_OK"));
        var future = StorePolicy.create(store.getId(), 3, "내일부터 테라스 허용", List.of());
        future.setConditions(conditions(7, true, EntryConditions.today().plusDays(1)));
        policies.saveAndFlush(future);
        mvc.perform(post(url).with(auth).contentType("application/json").content(body))
                .andExpect(jsonPath("$.overallVerdict").value("ENTRY_OK"));
        assertThat(policies.findActive(store.getId()).orElseThrow().getVersion()).isEqualTo(2);
        // Once effective, both dogs must share one permitted area, and the detail reflects that
        // area's limit.
        future.setConditions(conditions(7, true, EntryConditions.today()));
        policies.saveAndFlush(future);
        String together = "{\"dogIds\":[" + mong.getId() + "," + kong.getId() + "]}";
        mvc.perform(post(url).with(auth).contentType("application/json").content(together))
                .andExpect(jsonPath("$.overallVerdict").value("ENTRY_OK"))
                .andExpect(jsonPath("$.area").value("OUTDOOR"));
        mvc.perform(
                        get("/api/v1/places/STORE:" + store.getId())
                                .with(auth)
                                .param("dogIds", mong.getId().toString(), kong.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entryArea").value("OUTDOOR"))
                .andExpect(jsonPath("$.maxWeightKg").doesNotExist())
                .andExpect(jsonPath("$.requiredEquipmentCodes[0]").value("LEASH"));
    }
}
