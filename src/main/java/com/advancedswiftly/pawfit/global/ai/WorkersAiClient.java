package com.advancedswiftly.pawfit.global.ai;

import com.advancedswiftly.pawfit.global.config.PawfitProperties;
import com.advancedswiftly.pawfit.global.http.ExternalRestClientFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

public class WorkersAiClient extends AbstractHttpAiClient {

    private static final String API_BASE = "https://api.cloudflare.com/client/v4/accounts/";
    private final PawfitProperties.Ai properties;
    private final RestClient restClient;
    private final AiResponseParser responseParser;

    public WorkersAiClient(PawfitProperties.Ai properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.restClient = ExternalRestClientFactory.create(properties.getTimeout());
        this.responseParser = new AiResponseParser(objectMapper);
    }

    @Override
    public AiProvider provider() {
        return AiProvider.WORKERS_AI;
    }

    @Override
    public VaccinationExtraction analyzeVaccination(MaskedDocument document) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("messages", List.of(
                Map.of("role", "system", "content", "Return valid JSON only."),
                Map.of("role", "user", "content", vaccinationPrompt())));
        body.put("image", "data:" + document.contentType() + ";base64,"
                + Base64.getEncoder().encodeToString(document.content()));
        return responseParser.parseVaccinationExtraction(run(properties.getWorkersAi().getVisionModel(), body));
    }

    @Override
    public String chat(AiChatRequest request) {
        List<Map<String, String>> messages = new ArrayList<>();
        if (request.systemPrompt() != null && !request.systemPrompt().isBlank()) {
            messages.add(Map.of("role", "system", "content", request.systemPrompt()));
        }
        request.messages().forEach(message -> messages.add(Map.of(
                "role", message.role(),
                "content", message.content())));
        return run(properties.getWorkersAi().getTextModel(), Map.of("messages", messages));
    }

    private String run(String model, Map<String, Object> body) {
        ensureConfigured();
        URI uri = URI.create(API_BASE + properties.getWorkersAi().getAccountId() + "/ai/run/" + model);
        try {
            JsonNode response = restClient.post()
                    .uri(uri)
                    .header("Authorization", "Bearer " + properties.getWorkersAi().getApiToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            JsonNode result = response == null ? null : response.path("result");
            String text = result == null
                    ? ""
                    : result.isTextual() ? result.asText() : result.path("response").asText("");
            if (text.isBlank()) {
                throw new AiClientException("Workers AI가 빈 응답을 반환했습니다.", false);
            }
            return text;
        } catch (RestClientException exception) {
            throw translate("Workers AI 요청", exception);
        }
    }

    private void ensureConfigured() {
        if (properties.getWorkersAi().getAccountId().isBlank()
                || properties.getWorkersAi().getApiToken().isBlank()) {
            throw new AiClientException(
                    "CLOUDFLARE_ACCOUNT_ID와 CLOUDFLARE_API_TOKEN을 설정해야 합니다.", false);
        }
    }
}
