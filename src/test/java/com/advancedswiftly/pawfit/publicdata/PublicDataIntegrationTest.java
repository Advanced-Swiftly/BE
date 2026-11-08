package com.advancedswiftly.pawfit.publicdata;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:publicdata;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class PublicDataIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate db;
    @MockitoBean PetTourClient client;
    @MockitoBean(name = "publicDataExecutor") ThreadPoolTaskExecutor executor;

    @Test
    void operatorCanStartSyncReadHistoryAndCorrectFacilities() throws Exception {
        db.execute("""
                CREATE TABLE public_facility(facility_id BIGINT PRIMARY KEY, source_code VARCHAR(40),
                name VARCHAR(150), latitude NUMERIC(10,7), longitude NUMERIC(10,7),
                category VARCHAR(80), visible BOOLEAN, region_id BIGINT)
                """);
        var migration = new ClassPathResource("db/migration/V12__add_public_data_sync.sql");
        for (String statement : migration.getContentAsString(StandardCharsets.UTF_8).split(";")) {
            if (!statement.isBlank()) db.execute(statement);
        }
        db.update("""
                INSERT INTO public_facility(facility_id,source_code,name,latitude,longitude,visible)
                VALUES(1801,'KTO_PET_TOUR','시설',37.5,127,FALSE)
                """);
        var task = new AtomicReference<Runnable>();
        doAnswer(call -> {
            task.set(call.getArgument(0));
            return null;
        }).when(executor).execute(any(Runnable.class));
        when(client.configured()).thenReturn(true);
        when(client.collect(null)).thenReturn(new PetTourClient.Batch(List.of(), 2));
        var operator = user("1").roles("OPERATOR");
        String source = "/api/v1/admin/public-data-sources";
        mvc.perform(get(source).with(user("2").roles("GUARDIAN"))).andExpect(status().isForbidden());
        mvc.perform(get(source).with(operator)).andExpect(status().isOk())
                .andExpect(jsonPath("$.sources[0].status").value("NEVER_SYNCED"));
        mvc.perform(post(source + "/1701/sync").with(operator)).andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("RUNNING"));
        mvc.perform(post(source + "/1701/sync").with(operator)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SYNC_ALREADY_RUNNING"));
        task.get().run();
        mvc.perform(get(source + "/1701/sync-runs").with(operator)).andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].status").value("SUCCESS"))
                .andExpect(jsonPath("$.items[0].failedCount").value(2));
        when(client.collect(null)).thenThrow(new IllegalStateException("외부 오류"));
        mvc.perform(post(source + "/1701/sync").with(operator).contentType("application/json")
                        .content("{\"fullSync\":true}")).andExpect(status().isAccepted());
        task.get().run();
        mvc.perform(get(source + "/1701/sync-runs").with(operator)).andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].status").value("FAILED"));
        String facilities = "/api/v1/admin/public-facilities";
        mvc.perform(patch(facilities + "/1801").with(operator).contentType("application/json")
                        .content("{\"name\":\"보정 시설\",\"visible\":true}")).andExpect(status().isOk())
                .andExpect(jsonPath("$.visible").value(true));
        mvc.perform(get(facilities).with(operator).param("sourceId", "1701").param("keyword", "보정"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].name").value("보정 시설"));
    }
}
