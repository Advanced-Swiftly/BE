package com.advancedswiftly.pawfit.global.ai;

import com.advancedswiftly.pawfit.global.config.PawfitProperties;
import com.advancedswiftly.pawfit.global.error.BusinessException;
import com.advancedswiftly.pawfit.global.error.ErrorCode;

public class AiService {

    private final AiClient aiClient;
    private final AiRetryExecutor retryExecutor;

    public AiService(AiClient aiClient, PawfitProperties.Ai properties) {
        this.aiClient = aiClient;
        this.retryExecutor = new AiRetryExecutor(properties.getMaxRetries(), properties.getRetryDelay());
    }

    public VaccinationVerificationState analyzeVaccination(MaskedDocument document) {
        try {
            VaccinationExtraction extraction = retryExecutor.execute(
                    () -> aiClient.analyzeVaccination(document));
            return VaccinationVerificationState.awaitingOperator(
                    VaccinationAnalysis.analyzed(aiClient.provider(), extraction));
        } catch (AiClientException exception) {
            return VaccinationVerificationState.awaitingOperator(
                    VaccinationAnalysis.needsManualReview(aiClient.provider(), exception.getMessage()));
        }
    }

    public AiChatResponse chat(AiChatRequest request) {
        try {
            String response = retryExecutor.execute(() -> aiClient.chat(request));
            return new AiChatResponse(aiClient.provider(), response);
        } catch (AiClientException exception) {
            throw new BusinessException(
                    ErrorCode.AI_UNAVAILABLE,
                    ErrorCode.AI_UNAVAILABLE.getMessage(),
                    exception);
        }
    }
}
