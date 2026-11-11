package com.advancedswiftly.pawfit.global.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

final class AiResponseParser {

    private final ObjectMapper objectMapper;

    AiResponseParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    VaccinationExtraction parseVaccinationExtraction(String response) {
        String json = stripCodeFence(response);
        try {
            return objectMapper.readValue(json, VaccinationExtraction.class);
        } catch (JsonProcessingException exception) {
            throw new AiClientException("AI 예방접종 분석 결과를 해석할 수 없습니다.", exception, false);
        }
    }

    private String stripCodeFence(String response) {
        if (response == null || response.isBlank()) {
            throw new AiClientException("AI가 빈 응답을 반환했습니다.", false);
        }
        String trimmed = response.trim();
        if (!trimmed.startsWith("```")) {
            return trimmed;
        }
        int firstLineEnd = trimmed.indexOf('\n');
        int closingFence = trimmed.lastIndexOf("```");
        if (firstLineEnd < 0 || closingFence <= firstLineEnd) {
            return trimmed;
        }
        return trimmed.substring(firstLineEnd + 1, closingFence).trim();
    }
}
