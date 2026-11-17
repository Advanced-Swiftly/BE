package com.advancedswiftly.pawfit.global.ai;

import com.advancedswiftly.pawfit.global.config.PawfitProperties;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tools.jackson.databind.ObjectMapper;

@Configuration
public class AiConfiguration {

    @Bean
    AiClient aiClient(PawfitProperties properties) {
        ObjectMapper objectMapper =
                tools.jackson.databind.json.JsonMapper.builder().findAndAddModules().build();
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
