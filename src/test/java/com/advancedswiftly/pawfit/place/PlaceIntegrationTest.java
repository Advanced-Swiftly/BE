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
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest(properties = {
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
        var owner = users.saveAndFlush(User.register("01012345678", "hash", "보호자", UserRole.GUARDIAN));
        var other = users.saveAndFlush(User.register("01098765432", "hash", "다른 보호자", UserRole.GUARDIAN));
        var breed = breeds.saveAndFlush(Breed.create("말티즈"));
        var small = dogs.saveAndFlush(Dog.create(owner, breed, "콩이", null,
                new BigDecimal("4.2"), true, List.of()));
        var large = dogs.saveAndFlush(Dog.create(owner, breed, "두부", null,
                new BigDecimal("12"), false, List.of()));
        db.update("""
                INSERT INTO store(store_id,name,address,latitude,longitude,verification_status,created_at,updated_at,category)
                VALUES(100,'멍카페','서울',37.5,127,'VERIFIED',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,'CAFE'),
                      (101,'숨김카페','서울',37.5,127,'PENDING',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,'CAFE')
                """);
        db.update("""
                INSERT INTO public_facility(facility_id,name,latitude,longitude,visible,category)
                VALUES(200,'공원',37.5,127,TRUE,'PARK')
                """);
        db.update("""
                INSERT INTO place_entry_policy VALUES('STORE:100',TRUE,10,'업체 공지',CURRENT_DATE)
                """);
        db.update("INSERT INTO place_entry_rule VALUES('STORE:100','EQUIPMENT','MUZZLE')");
        var guardian = user(owner.getId().toString()).roles("GUARDIAN");
        mvc.perform(get("/api/v1/places").with(guardian).param("keyword", "카페")
                        .param("dogIds", small.getId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].verdict").value("PREPARE"));
        mvc.perform(get("/api/v1/places/map").with(guardian).param("latitude", "37.5")
                        .param("longitude", "127").param("category", "PARK"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.places[0].placeId").value("PUBLIC:200"));
        mvc.perform(get("/api/v1/places/map").with(guardian).param("latitude", "oops"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/places/PUBLIC:200").with(guardian)
                        .param("dogIds", small.getId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.verdict").value("CHECK_REQUIRED"))
                .andExpect(jsonPath("$.googleMapsUrl")
                        .value("https://www.google.com/maps/search/?api=1&query=37.5%2C127"));
        mvc.perform(get("/api/v1/places/STORE:101").with(guardian))
                .andExpect(status().isNotFound());
        String selection = "{\"dogIds\":[%d,%d]}".formatted(small.getId(), large.getId());
        mvc.perform(post("/api/v1/places/STORE:100/entry-judgments").with(guardian)
                        .contentType("application/json").content(selection))
                .andExpect(status().isOk()).andExpect(jsonPath("$.overallVerdict").value("ENTRY_DENIED"))
                .andExpect(jsonPath("$.dogs[0].reasons[0]").value("MUZZLE_REQUIRED"));
        mvc.perform(post("/api/v1/places/STORE:100/entry-judgments")
                        .with(user(other.getId().toString()).roles("GUARDIAN"))
                        .contentType("application/json").content(selection))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("DOG_NOT_OWNED"));
    }

    private void apply(String migration) throws Exception {
        var resource = new ClassPathResource("db/migration/" + migration);
        for (String sql : resource.getContentAsString(StandardCharsets.UTF_8).split(";")) {
            if (!sql.isBlank() && !sql.contains("ENABLE ROW LEVEL SECURITY")) db.execute(sql.replace("ADD COLUMN region_id", "ADD COLUMN IF NOT EXISTS region_id"));
        }
    }
}
