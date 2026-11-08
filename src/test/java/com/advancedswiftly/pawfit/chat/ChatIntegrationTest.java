package com.advancedswiftly.pawfit.chat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.advancedswiftly.pawfit.auth.domain.User;
import com.advancedswiftly.pawfit.auth.domain.UserRole;
import com.advancedswiftly.pawfit.auth.infrastructure.UserRepository;
import com.advancedswiftly.pawfit.global.ai.AiChatResponse;
import com.advancedswiftly.pawfit.global.ai.AiProvider;
import com.advancedswiftly.pawfit.global.ai.AiService;
import com.advancedswiftly.pawfit.global.error.BusinessException;
import com.advancedswiftly.pawfit.global.error.ErrorCode;
import com.advancedswiftly.pawfit.place.PlaceService;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:chat;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class ChatIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate db;
    @Autowired UserRepository users;
    @MockitoBean AiService ai;
    @MockitoBean PlaceService places;

    @Test
    void ownsConversationAndStoresOnlySuccessfulTurnsWithVerifiedPlaceIds() throws Exception {
        var migration = new ClassPathResource("db/migration/V13__create_chat_tables.sql");
        for (String statement : migration.getContentAsString(StandardCharsets.UTF_8).split(";")) {
            if (!statement.isBlank() && !statement.contains("ENABLE ROW LEVEL SECURITY")) db.execute(statement);
        }
        var owner = users.saveAndFlush(User.register("01012345678", "hash", "보호자", UserRole.GUARDIAN));
        var guardian = user(owner.getId().toString()).roles("GUARDIAN");
        String root = "/api/v1/chat/sessions";
        var created = mvc.perform(post(root).with(guardian))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.title").value("새 대화")).andReturn();
        Number id = JsonPath.read(created.getResponse().getContentAsString(), "$.sessionId");
        String session = root + "/" + id;
        String messages = session + "/messages";
        mvc.perform(get(root).with(guardian)).andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].sessionId").value(id.longValue()));
        mvc.perform(get(messages).with(user("999").roles("GUARDIAN"))).andExpect(status().isNotFound());
        mvc.perform(get(root).with(user("999").roles("BUSINESS"))).andExpect(status().isForbidden());

        when(places.chatCandidates(any(), isNull(), isNull(), isNull())).thenReturn(List.of(
                new PlaceService.Card("STORE:100", "멍카페", null, null, PlaceService.Verdict.PREPARE)));
        when(ai.chat(any())).thenReturn(new AiChatResponse(AiProvider.GEMINI,
                """
                {"content":"입마개 준비가 필요해요.","placeIds":["STORE:999","STORE:100"]}
                """)).thenThrow(new BusinessException(ErrorCode.AI_UNAVAILABLE));
        String request = "{\"content\":\"갈 수 있는 카페가 있을까?\"}";
        mvc.perform(post(messages).with(guardian).contentType("application/json").content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.assistantMessage.places.length()").value(1))
                .andExpect(jsonPath("$.assistantMessage.places[0].verdict").value("PREPARE"));
        mvc.perform(post(messages).with(guardian).contentType("application/json").content(request))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("CHAT_SERVICE_UNAVAILABLE"));
        var history = mvc.perform(get(messages).with(guardian)).andExpect(status().isOk())
                .andExpect(jsonPath("$.messages.length()").value(2))
                .andExpect(jsonPath("$.messages[0].role").value("USER")).andReturn();
        Number assistant = JsonPath.read(history.getResponse().getContentAsString(), "$.messages[1].messageId");
        mvc.perform(get(messages).with(guardian).param("cursor", assistant.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.messages.length()").value(1));
        mvc.perform(delete(session).with(guardian)).andExpect(status().isNoContent());
        mvc.perform(get(messages).with(guardian)).andExpect(status().isNotFound());
    }
}
