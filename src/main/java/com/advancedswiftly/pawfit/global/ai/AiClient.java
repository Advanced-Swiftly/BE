package com.advancedswiftly.pawfit.global.ai;

public interface AiClient {

    AiProvider provider();

    VaccinationExtraction analyzeVaccination(MaskedDocument document);

    String chat(AiChatRequest request);
}
