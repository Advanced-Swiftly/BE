package com.advancedswiftly.pawfit.global.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.advancedswiftly.pawfit.global.config.PawfitProperties;
import java.net.SocketTimeoutException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

class GeminiAiClientTest {
    private static final String BASE = "https://generativelanguage.googleapis.com/v1beta/models/";
    private static final String PRIMARY = "gemini-3.8-flash";
    private static final String FALLBACK = "gemini-3.5-flash-lite";
    private static final String RESPONSE = """
            {"candidates":[{"content":{"parts":[{"text":"{\\\"content\\\":\\\"안내\\\",\\\"placeIds\\\":[]}"}]}}]}
            """;
    private static final String REQUEST = """
            {"contents":[{"role":"user","parts":[{"text":"가까운 카페"}]},
                         {"role":"model","parts":[{"text":"몽이와 갈 곳을 찾습니다."}]},
                         {"role":"user","parts":[{"text":"공공데이터도 알려줘"}]}],
             "systemInstruction":{"parts":[{"text":"후보의 판정만 사용하세요."}]},
             "generationConfig":{"responseMimeType":"application/json"}}
            """;
    private final AiChatRequest chat = new AiChatRequest("후보의 판정만 사용하세요.", List.of(
            new AiChatMessage("user", "가까운 카페"),
            new AiChatMessage("assistant", "몽이와 갈 곳을 찾습니다."),
            new AiChatMessage("user", "공공데이터도 알려줘")));
    private PawfitProperties.Ai properties;
    private MockRestServiceServer server;
    private GeminiAiClient client;

    @BeforeEach
    void setUp() {
        properties = new PawfitProperties.Ai();
        properties.getGemini().setApiKey("test-key");
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new GeminiAiClient(properties, JsonMapper.builder().build(), builder.build());
    }

    @Test
    void successfulPrimaryDoesNotUseFallback() {
        server.expect(requestTo(url(PRIMARY)))
                .andExpect(header("x-goog-api-key", "test-key"))
                .andExpect(content().json(REQUEST))
                .andRespond(withSuccess(RESPONSE, MediaType.APPLICATION_JSON));
        assertThat(client.chat(chat)).contains("안내");
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(ints = {429, 503})
    void temporaryFailurePreservesContextAndUsesFallback(int status) {
        server.expect(requestTo(url(PRIMARY))).andRespond(withStatus(HttpStatus.valueOf(status)));
        server.expect(requestTo(url(FALLBACK)))
                .andExpect(header("x-goog-api-key", "test-key"))
                .andExpect(content().json(REQUEST))
                .andRespond(withSuccess(RESPONSE, MediaType.APPLICATION_JSON));
        assertThat(client.chat(chat)).contains("안내");
        server.verify();
    }

    @Test
    void timeoutCanUseFallback() {
        server.expect(requestTo(url(PRIMARY)))
                .andRespond(withException(new SocketTimeoutException("timeout")));
        server.expect(requestTo(url(FALLBACK)))
                .andRespond(withSuccess(RESPONSE, MediaType.APPLICATION_JSON));
        assertThat(client.chat(chat)).contains("안내");
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 403, 404})
    void configurationAndAccessErrorsDoNotUseFallback(int status) {
        server.expect(requestTo(url(PRIMARY))).andRespond(withStatus(HttpStatus.valueOf(status)));
        assertFailure(false);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", PRIMARY})
    void disabledOrIdenticalFallbackDoesNotDuplicateRequest(String fallback) {
        properties.getGemini().setChatFallbackModel(fallback);
        server.expect(requestTo(url(PRIMARY))).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        assertFailure(true);
    }

    @Test
    void bothModelsFailWithoutUnboundedFallback() {
        server.expect(requestTo(url(PRIMARY))).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        server.expect(requestTo(url(FALLBACK))).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        assertFailure(true);
    }

    @Test
    void vaccinationAnalysisKeepsPrimaryModel() {
        server.expect(requestTo(url(PRIMARY))).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        assertThatThrownBy(() -> client.analyzeVaccination(new MaskedDocument(new byte[] {1}, "image/png")))
                .isInstanceOf(AiClientException.class);
        server.verify();
    }

    private void assertFailure(boolean retryable) {
        assertThatThrownBy(() -> client.chat(chat))
                .isInstanceOfSatisfying(AiClientException.class,
                        exception -> assertThat(exception.isRetryable()).isEqualTo(retryable));
        server.verify();
    }

    private String url(String model) {
        return BASE + model + ":generateContent";
    }
}
