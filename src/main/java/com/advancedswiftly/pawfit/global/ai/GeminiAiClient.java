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

public class GeminiAiClient extends AbstractHttpAiClient {

    private static final String API_BASE = "https://generativelanguage.googleapis.com/v1beta/models/";
    private final PawfitProperties.Ai properties;
    private final RestClient restClient;
    private final AiResponseParser responseParser;

    public GeminiAiClient(PawfitProperties.Ai properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.restClient = ExternalRestClientFactory.create(properties.getTimeout());
        this.responseParser = new AiResponseParser(objectMapper);
    }

    @Override
    public AiProvider provider() {
        return AiProvider.GEMINI;
    }

    @Override
    public VaccinationExtraction analyzeVaccination(MaskedDocument document) {
        List<Map<String, Object>> parts = List.of(
                Map.of("text", vaccinationPrompt()),
                Map.of("inlineData", Map.of(
                        "mimeType", document.contentType(),
                        "data", Base64.getEncoder().encodeToString(document.content()))));
        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of("role", "user", "parts", parts)),
                "generationConfig", Map.of("responseMimeType", "application/json"));
        return responseParser.parseVaccinationExtraction(generate(body));
    }

    @Override
    public String chat(AiChatRequest request) {
        List<Map<String, Object>> contents = new ArrayList<>();
        for (AiChatMessage message : request.messages()) {
            String role = "assistant".equalsIgnoreCase(message.role()) ? "model" : "user";
            contents.add(Map.of("role", role, "parts", List.of(Map.of("text", message.content()))));
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("contents", contents);
        if (request.systemPrompt() != null && !request.systemPrompt().isBlank()) {
            body.put("systemInstruction", Map.of("parts", List.of(Map.of("text", request.systemPrompt()))));
        }
        return generate(body);
    }

    private String generate(Map<String, Object> body) {
        ensureConfigured();
        URI uri = URI.create(API_BASE + properties.getGemini().getModel() + ":generateContent");
        try {
            JsonNode response = restClient.post()
                    .uri(uri)
                    .header("x-goog-api-key", properties.getGemini().getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            String text = response == null
                    ? ""
                    : response.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText("");
            if (text.isBlank()) {
                throw new AiClientException("Gemini가 빈 응답을 반환했습니다.", false);
            }
            return text;
        } catch (RestClientException exception) {
            throw translate("Gemini 요청", exception);
        }
    }

    private void ensureConfigured() {
        if (properties.getGemini().getApiKey().isBlank()) {
            throw new AiClientException("GEMINI_API_KEY를 설정해야 합니다.", false);
        }
    }
}
