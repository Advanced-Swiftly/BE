package com.advancedswiftly.pawfit.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.advancedswiftly.pawfit.auth.domain.*;
import com.advancedswiftly.pawfit.auth.infrastructure.*;
import com.advancedswiftly.pawfit.dog.domain.Dog;
import com.advancedswiftly.pawfit.dog.infrastructure.DogRepository;
import com.advancedswiftly.pawfit.global.ai.AiService;
import com.advancedswiftly.pawfit.place.PlaceService;
import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:chatnamed;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
            "spring.jpa.hibernate.ddl-auto=create-drop"
        })
class ChatNamedPlaceIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate db;
    @Autowired UserRepository users;
    @Autowired BreedRepository breeds;
    @Autowired DogRepository dogs;
    @Autowired PlaceService places;
    @MockitoBean AiService ai;

    @Test
    void findsNamedPlaceBeyondTwelveAndDistanceAndReadsFreshPolicyWithoutAi() throws Exception {
        for (String file :
                List.of(
                        "V8__create_vaccination_tables.sql",
                        "V9__create_place_catalog.sql",
                        "V13__create_chat_tables.sql")) {
            for (String sql :
                    new ClassPathResource("db/migration/" + file)
                            .getContentAsString(StandardCharsets.UTF_8)
                            .split(";"))
                if (!sql.isBlank() && !sql.contains("ENABLE ROW LEVEL SECURITY"))
                    db.execute(
                            sql.replace(
                                    "ADD COLUMN region_id", "ADD COLUMN IF NOT EXISTS region_id"));
        }
        db.execute(
                "ALTER TABLE place_entry_policy ADD COLUMN details_complete BOOLEAN NOT NULL"
                    + " DEFAULT TRUE");
        db.execute(
                "ALTER TABLE place_entry_policy ADD COLUMN max_weight_exclusive BOOLEAN NOT NULL"
                    + " DEFAULT FALSE");
        var owner =
                users.saveAndFlush(User.register("01011110001", "hash", "보호자", UserRole.GUARDIAN));
        var other =
                users.saveAndFlush(
                        User.register("01011110002", "hash", "다른 보호자", UserRole.GUARDIAN));
        var breed = breeds.saveAndFlush(Breed.create("말티즈"));
        var mong =
                dogs.saveAndFlush(
                        Dog.create(
                                owner,
                                breed,
                                "몽이",
                                null,
                                new BigDecimal("4.2"),
                                true,
                                List.of("STROLLER")));
        var foreign =
                dogs.saveAndFlush(
                        Dog.create(
                                other, breed, "콩이", null, new BigDecimal("12"), true, List.of()));
        for (int i = 1; i <= 14; i++) {
            db.update(
                    "INSERT INTO"
                        + " store(store_id,name,address,latitude,longitude,verification_status,category,created_at,updated_at)"
                        + " VALUES(?,?,?,37.54,127.05,'VERIFIED','CAFE',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)",
                    1000 + i,
                    i == 14 ? "테라스 오브 성수" : "AAA카페" + i,
                    "시연 위치");
            db.update(
                    "INSERT INTO"
                        + " place_entry_policy(place_id,pets_allowed,max_weight_kg,source_text,confirmed_on)"
                        + " VALUES(?,TRUE,10,'시연 정책',CURRENT_DATE)",
                    "STORE:" + (1000 + i));
        }
        db.update("INSERT INTO place_entry_rule VALUES('STORE:1014','EQUIPMENT','STROLLER')");
        String policy =
                "{\"petsAllowed\":true,\"indoor\":{\"allowed\":true,\"maxWeightKg\":10,\"equipmentCodes\":[\"STROLLER\"]},\"outdoor\":{\"allowed\":false,\"equipmentCodes\":[]}}";
        db.update(
                "INSERT INTO"
                    + " store_policy(store_id,version,source_text,conditions_json,created_at,updated_at)"
                    + " VALUES(1014,1,'승인 정책',?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)",
                policy);

        var auth =
                new UsernamePasswordAuthenticationToken(
                        owner.getId().toString(),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_GUARDIAN")));
        assertThat(places.chatCandidates(auth, List.of(mong.getId()), null, null))
                .noneMatch(p -> p.placeId().equals("STORE:1014"));
        assertThat(places.namedChatPlaces(auth, List.of(mong.getId()), "테라스오브성수에 몽이 데려가도 돼?"))
                .singleElement()
                .satisfies(
                        p ->
                                assertThat(p.card().verdict())
                                        .isEqualTo(PlaceService.Verdict.ENTRY_OK));
        var guardian = user(owner.getId().toString()).roles("GUARDIAN");
        String root = "/api/v1/chat/sessions";
        String created =
                mvc.perform(post(root).with(guardian))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        Number id = JsonPath.read(created, "$.sessionId");
        String messages = root + "/" + id + "/messages";
        String request = "{\"content\":\"테라스 오브 성수에 몽이 데려가도 돼?\",\"latitude\":0,\"longitude\":0}";
        mvc.perform(post(messages).with(guardian).contentType("application/json").content(request))
                .andExpect(status().isCreated())
                .andExpect(
                        jsonPath("$.assistantMessage.content")
                                .value(org.hamcrest.Matchers.containsString("몽이 기준으로 입장 가능")))
                .andExpect(jsonPath("$.assistantMessage.places[0].placeId").value("STORE:1014"));
        db.update(
                "INSERT INTO"
                    + " store_policy(store_id,version,source_text,conditions_json,created_at,updated_at)"
                    + " VALUES(1014,2,'변경 승인 정책',?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)",
                policy.replace("STROLLER", "MUZZLE"));
        mvc.perform(post(messages).with(guardian).contentType("application/json").content(request))
                .andExpect(status().isCreated())
                .andExpect(
                        jsonPath("$.assistantMessage.content")
                                .value(org.hamcrest.Matchers.containsString("현재 프로필에 없는 준비물: 입마개")))
                .andExpect(jsonPath("$.assistantMessage.places[0].verdict").value("PREPARE"));
        mvc.perform(
                        post(messages)
                                .with(guardian)
                                .contentType("application/json")
                                .content(
                                        "{\"content\":\"테라스 오브 성수 입장 가능?\",\"selectedDogIds\":["
                                                + foreign.getId()
                                                + "]}"))
                .andExpect(status().isForbidden());
        db.update("UPDATE place_entry_policy SET pets_allowed=FALSE WHERE place_id='STORE:1014'");
        db.update(
                "UPDATE store_policy SET conditions_json=? WHERE store_id=1014 AND version=2",
                policy.replace("true", "false"));
        assertThat(places.namedChatPlaces(auth, List.of(mong.getId()), "테라스 오브 성수에 가도 돼?"))
                .isEmpty();
        verifyNoInteractions(ai);
    }
}
