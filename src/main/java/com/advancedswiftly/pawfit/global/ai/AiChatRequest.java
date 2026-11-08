package com.advancedswiftly.pawfit.global.ai;

import java.util.List;

public record AiChatRequest(String systemPrompt, List<AiChatMessage> messages) {

    public AiChatRequest {
        messages = messages == null ? List.of() : List.copyOf(messages);
    }
}
