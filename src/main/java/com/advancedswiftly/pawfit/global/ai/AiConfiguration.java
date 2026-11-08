package com.advancedswiftly.pawfit.global.ai;

import com.advancedswiftly.pawfit.global.config.PawfitProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfiguration {

    @Bean
    AiClient aiClient(PawfitProperties properties) {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        return switch (properties.getAi().getProvider()) {
            case GEMINI -> new GeminiAiClient(properties.getAi(), objectMapper);
            case WORKERS_AI -> new WorkersAiClient(properties.getAi(), objectMapper);
        };
    }

    @Bean
    AiService aiService(AiClient aiClient, PawfitProperties properties) {
        return new AiService(aiClient, properties.getAi());
    }
}
