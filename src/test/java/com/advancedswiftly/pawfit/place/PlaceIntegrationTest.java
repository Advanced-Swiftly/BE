package com.advancedswiftly.pawfit.place;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.advancedswiftly.pawfit.auth.domain.Breed;
import com.advancedswiftly.pawfit.auth.domain.User;
import com.advancedswiftly.pawfit.auth.domain.UserRole;
import com.advancedswiftly.pawfit.auth.infrastructure.BreedRepository;
import com.advancedswiftly.pawfit.auth.infrastructure.UserRepository;
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
            "spring.datasource.url=jdbc:h2:mem:places;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
            "spring.jpa.hibernate.ddl-auto=create-drop"
        })
class PlaceIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate db;
    @Autowired UserRepository users;
    @Autowired BreedRepository breeds;
    @Autowired DogRepository dogs;

    @Test
    void searchesMapsAndUsesWorstOwnedDogVerdict() throws Exception {
        apply("V8__create_vaccination_tables.sql");
        apply("V9__create_place_catalog.sql");
        db.execute("ALTER TABLE public_facility ADD COLUMN phone VARCHAR(80)");
        db.execute("ALTER TABLE public_facility ADD COLUMN business_hours VARCHAR(1000)");
        var owner =
                users.saveAndFlush(User.register("01012345678", "hash", "보호자", UserRole.GUARDIAN));
        var other =
                users.saveAndFlush(
                        User.register("01098765432", "hash", "다른 보호자", UserRole.GUARDIAN));
        var breed = breeds.saveAndFlush(Breed.create("말티즈"));
        var small =
                dogs.saveAndFlush(
                        Dog.create(
                                owner, breed, "콩이", null, new BigDecimal("4.2"), true, List.of()));
        var large =
                dogs.saveAndFlush(
                        Dog.create(
                                owner, breed, "두부", null, new BigDecimal("12"), false, List.of()));
        db.update(
                """
INSERT INTO store(store_id,name,address,latitude,longitude,verification_status,created_at,updated_at,category)
VALUES(100,'멍카페','서울',37.5,127,'VERIFIED',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,'CAFE'),
      (101,'숨김카페','서울',37.5,127,'PENDING',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,'CAFE')
""");
        db.update(
                """
                INSERT INTO public_facility(facility_id,name,latitude,longitude,visible,category)
                VALUES(200,'공원',37.5,127,TRUE,'PARK')
                """);
        db.update(
                """
                INSERT INTO place_entry_policy(place_id,pets_allowed,max_weight_kg,source_text,confirmed_on)
                VALUES('STORE:100',TRUE,10,'업체 공지',CURRENT_DATE)
                """);
        db.update("INSERT INTO place_entry_rule VALUES('STORE:100','EQUIPMENT','MUZZLE')");
        db.update("INSERT INTO place_entry_policy(place_id,pets_allowed,source_text,confirmed_on,details_complete) VALUES('PUBLIC:200',TRUE,'동반 가능, 세부 조건 미확인',CURRENT_DATE,FALSE)");
        var guardian = user(owner.getId().toString()).roles("GUARDIAN");
        mvc.perform(
                        get("/api/v1/places")
                                .with(guardian)
                                .param("keyword", "카페")
                                .param("dogIds", small.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].verdict").value("PREPARE"));
        mvc.perform(
                        get("/api/v1/places/map")
                                .with(guardian)
                                .param("latitude", "37.5")
                                .param("longitude", "127")
                                .param("category", "PARK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.places[0].placeId").value("PUBLIC:200"));
        mvc.perform(get("/api/v1/places/map").with(guardian).param("latitude", "oops"))
                .andExpect(status().isBadRequest());
        mvc.perform(
                        get("/api/v1/places/PUBLIC:200")
                                .with(guardian)
                                .param("dogIds", small.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("CHECK_REQUIRED"))
                .andExpect(jsonPath("$.admissionConfirmed").value(true))
                .andExpect(jsonPath("$.policyDetailsComplete").value(false))
                .andExpect(jsonPath("$.unrestrictedFields.length()").value(0))
                .andExpect(
                        jsonPath("$.googleMapsUrl")
                                .value(
                                        "https://www.google.com/maps/search/?api=1&query=37.5%2C127"));
        mvc.perform(get("/api/v1/places/STORE:101").with(guardian))
                .andExpect(status().isNotFound());
        String selection = "{\"dogIds\":[%d,%d]}".formatted(small.getId(), large.getId());
        mvc.perform(
                        post("/api/v1/places/STORE:100/entry-judgments")
                                .with(guardian)
                                .contentType("application/json")
                                .content(selection))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overallVerdict").value("ENTRY_DENIED"))
                .andExpect(jsonPath("$.dogs[0].reasons[0]").value("MUZZLE_REQUIRED"));
        mvc.perform(
                        post("/api/v1/places/STORE:100/entry-judgments")
                                .with(user(other.getId().toString()).roles("GUARDIAN"))
                                .contentType("application/json")
                                .content(selection))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("DOG_NOT_OWNED"));

        db.update("""
                INSERT INTO public_facility(facility_id,name,latitude,longitude,visible,category)
                VALUES(201,'실제 찻집',37.5,127,TRUE,'CAFE'),
                      (202,'음식점',37.5,127,TRUE,'FOOD'),
                      (203,'기존 음식점',37.5,127,TRUE,'RESTAURANT')
                """);
        for (int id : List.of(201, 202, 203)) {
            db.update("INSERT INTO place_entry_policy(place_id,pets_allowed,source_text,confirmed_on,details_complete) VALUES(?,TRUE,'동반 확인',CURRENT_DATE,FALSE)", "PUBLIC:" + id);
        }
        for (String category : List.of("카페", "CAFE")) {
            mvc.perform(get("/api/v1/places/map").with(guardian)
                            .param("latitude", "37.5").param("longitude", "127")
                            .param("category", category))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.places.length()").value(2));
            mvc.perform(get("/api/v1/places").with(guardian).param("category", category))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(2));
        }
        for (String category : List.of("식당", "FOOD", "RESTAURANT")) {
            mvc.perform(get("/api/v1/places/map").with(guardian)
                            .param("latitude", "37.5").param("longitude", "127")
                            .param("category", category))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.places.length()").value(2));
            mvc.perform(get("/api/v1/places").with(guardian).param("category", category))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(2));
        }

        // Unknown admission and explicit denial must never appear as gray pins.
        db.update("INSERT INTO public_facility(facility_id,name,latitude,longitude,visible,category) VALUES(204,'동반 미확인',37.5,127,TRUE,'PARK'),(205,'동반 불가',37.5,127,TRUE,'PARK')");
        db.update("INSERT INTO place_entry_policy(place_id,pets_allowed,source_text,confirmed_on) VALUES('PUBLIC:205',FALSE,'동반 불가 확인',CURRENT_DATE)");
        for (int id : List.of(204, 205)) {
            mvc.perform(get("/api/v1/places/PUBLIC:" + id).with(guardian))
                    .andExpect(status().isNotFound());
        }
        mvc.perform(get("/api/v1/places/map").with(guardian).param("latitude", "37.5").param("longitude", "127").param("category", "PARK"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.places.length()").value(1));
        mvc.perform(get("/api/v1/places").with(guardian).param("category", "PARK"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1));

        // An incomplete policy still rejects a known hard limit; 7kg 미만 is strict.
        db.update("UPDATE place_entry_policy SET max_weight_kg=4.2,max_weight_exclusive=TRUE WHERE place_id='PUBLIC:200'");
        mvc.perform(get("/api/v1/places/PUBLIC:200").with(guardian).param("dogIds", small.getId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.verdict").value("ENTRY_DENIED"))
                .andExpect(jsonPath("$.maxWeightExclusive").value(true));
        db.update("UPDATE place_entry_policy SET max_weight_kg=7 WHERE place_id='PUBLIC:200'");
        mvc.perform(get("/api/v1/places/PUBLIC:200").with(guardian).param("dogIds", small.getId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.verdict").value("CHECK_REQUIRED"));
    }

    private void apply(String migration) throws Exception {
        var resource = new ClassPathResource("db/migration/" + migration);
        for (String sql : resource.getContentAsString(StandardCharsets.UTF_8).split(";")) {
            if (!sql.isBlank() && !sql.contains("ENABLE ROW LEVEL SECURITY"))
                db.execute(
                        sql.replace("ADD COLUMN region_id", "ADD COLUMN IF NOT EXISTS region_id"));
        }
        if (migration.startsWith("V9__")) {
            db.execute("ALTER TABLE place_entry_policy ADD COLUMN details_complete BOOLEAN NOT NULL DEFAULT TRUE");
            db.execute("ALTER TABLE place_entry_policy ADD COLUMN max_weight_exclusive BOOLEAN NOT NULL DEFAULT FALSE");
            db.execute("CREATE OR REPLACE VIEW place_catalog AS SELECT CONCAT('STORE:',s.store_id) AS place_id,s.name,s.address,s.region_id,s.category,s.latitude,s.longitude FROM store s JOIN place_entry_policy p ON p.place_id=CONCAT('STORE:',s.store_id) WHERE s.verification_status='VERIFIED' AND p.pets_allowed=TRUE UNION ALL SELECT CONCAT('PUBLIC:',f.facility_id),f.name,f.address,f.region_id,f.category,f.latitude,f.longitude FROM public_facility f JOIN place_entry_policy p ON p.place_id=CONCAT('PUBLIC:',f.facility_id) WHERE f.visible=TRUE AND p.pets_allowed=TRUE AND p.confirmed_on<=CURRENT_DATE");
        }
    }
}
