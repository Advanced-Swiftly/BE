package com.advancedswiftly.pawfit.global.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.advancedswiftly.pawfit.global.config.PawfitProperties;
import com.advancedswiftly.pawfit.global.error.BusinessException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class AiServiceTest {

    @Test
    void separatesAiAnalysisFromOperatorReview() {
        AiClient client = new StubAiClient(false);
        AiService service = new AiService(client, testProperties(0));

        VaccinationVerificationState state = service.analyzeVaccination(
                new MaskedDocument(new byte[] {1}, "image/png"));

        assertThat(state.aiAnalysis().status()).isEqualTo(VaccinationAnalysisStatus.ANALYZED);
        assertThat(state.operatorReviewStatus()).isEqualTo(OperatorReviewStatus.PENDING);
    }

    @Test
    void retriesTransientFailuresThenRequestsManualReview() {
        StubAiClient client = new StubAiClient(true);
        AiService service = new AiService(client, testProperties(2));

        VaccinationVerificationState state = service.analyzeVaccination(
                new MaskedDocument(new byte[] {1}, "image/png"));

        assertThat(client.attempts()).isEqualTo(3);
        assertThat(state.aiAnalysis().status())
                .isEqualTo(VaccinationAnalysisStatus.NEEDS_MANUAL_REVIEW);
        assertThat(state.operatorReviewStatus()).isEqualTo(OperatorReviewStatus.PENDING);
    }

    @Test
    void returnsServiceUnavailableWhenChatRetriesAreExhausted() {
        StubAiClient client = new StubAiClient(true);
        AiService service = new AiService(client, testProperties(1));

        assertThatThrownBy(() -> service.chat(new AiChatRequest(
                        "system", List.of(new AiChatMessage("user", "question")))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("일시적으로");
    }

    private PawfitProperties.Ai testProperties(int maxRetries) {
        PawfitProperties.Ai properties = new PawfitProperties.Ai();
        properties.setMaxRetries(maxRetries);
        properties.setRetryDelay(Duration.ZERO);
        return properties;
    }

    private static final class StubAiClient implements AiClient {

        private final boolean fail;
        private final AtomicInteger attempts = new AtomicInteger();

        private StubAiClient(boolean fail) {
            this.fail = fail;
        }

        @Override
        public AiProvider provider() {
            return AiProvider.GEMINI;
        }

        @Override
        public VaccinationExtraction analyzeVaccination(MaskedDocument document) {
            attempts.incrementAndGet();
            if (fail) {
                throw new AiClientException("temporary", true);
            }
            return new VaccinationExtraction(
                    "Paw",
                    "Clinic",
                    List.of(new VaccinationItem("Rabies", "2026-10-01")),
                    0.9);
        }

        @Override
        public String chat(AiChatRequest request) {
            attempts.incrementAndGet();
            if (fail) {
                throw new AiClientException("temporary", true);
            }
            return "ok";
        }

        int attempts() {
            return attempts.get();
        }
    }
}
