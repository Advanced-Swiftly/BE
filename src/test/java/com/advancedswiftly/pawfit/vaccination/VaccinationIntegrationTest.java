package com.advancedswiftly.pawfit.vaccination;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.advancedswiftly.pawfit.global.ai.*;
import com.advancedswiftly.pawfit.global.storage.TransactionalStorageService;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:vaccination;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class VaccinationIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate db;
    @MockitoBean TransactionalStorageService storage;
    @MockitoBean AiService ai;
    @MockitoBean(name = "vaccinationExecutor") ThreadPoolTaskExecutor executor;

    @Test
    void guardianCanUploadReadResubmitAndCorrect() throws Exception {
        var resource = new ClassPathResource("db/migration/V8__create_vaccination_tables.sql");
        for (String sql : resource.getContentAsString(StandardCharsets.UTF_8).split(";")) {
            if (!sql.isBlank() && !sql.strip().startsWith("ALTER TABLE")) db.execute(sql);
        }
        db.update("""
                INSERT INTO app_user(user_id, phone_number, password_hash, name, role, status, created_at, updated_at)
                VALUES (1, '01012345678', 'hash', '보호자', 'GUARDIAN', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);
        db.update("INSERT INTO breed(breed_id, name, active) VALUES (1, '말티즈', TRUE)");
        db.update("""
                INSERT INTO dog(dog_id, user_id, breed_id, name, is_default, vaccination_status, created_at, updated_at)
                VALUES (1, 1, 1, '콩이', TRUE, 'NOT_SUBMITTED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);
        doAnswer(call -> {
            call.getArgument(0, Runnable.class).run();
            return null;
        }).when(executor).execute(any(Runnable.class));
        when(ai.analyzeVaccination(any())).thenReturn(
                VaccinationVerificationState.awaitingOperator(
                        VaccinationAnalysis.analyzed(AiProvider.GEMINI, new VaccinationExtraction(
                                null, null, List.of(new VaccinationItem("광견병", "2025-03-01")), 0.97))));
        var guardian = user("1").roles("GUARDIAN");
        String record = "/api/v1/vaccination-records/1";
        var file = new MockMultipartFile("files", "certificate.pdf", "application/pdf",
                "%PDF-test".getBytes(StandardCharsets.UTF_8));
        mvc.perform(multipart("/api/v1/dogs/1/vaccination-documents").file(file).with(guardian))
                .andExpect(status().isAccepted()).andExpect(jsonPath("$.documents[0].documentId").isNumber());
        mvc.perform(get("/api/v1/dogs/1/vaccination-record").with(guardian))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].vaccineCode").value("RABIES"));
        mvc.perform(get(record).with(user("2").roles("GUARDIAN"))).andExpect(status().isBadRequest());
        mvc.perform(get(record).with(guardian))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].confidence").value(0.97));
        mvc.perform(multipart(record + "/resubmit").file(file).with(guardian))
                .andExpect(status().isAccepted());
        mvc.perform(patch(record).with(guardian).contentType("application/json")
                        .content("""
                                {"items":[{"vaccineCode":"RABIES","vaccinatedAt":"2025-03-02"}]}
                                """))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("OPERATOR_REVIEW"));
        mvc.perform(get(record).with(guardian))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].vaccinatedAt").value("2025-03-01"));
        mvc.perform(multipart(record + "/resubmit").file(file).with(guardian))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("RESUBMIT_NOT_ALLOWED"));
    }
}
