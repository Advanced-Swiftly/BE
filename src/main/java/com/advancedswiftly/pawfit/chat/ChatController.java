package com.advancedswiftly.pawfit.chat;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@PreAuthorize("hasRole('GUARDIAN')")
@RequestMapping("/api/v1/chat/sessions")
class ChatController {
    private final ChatService chat;

    ChatController(ChatService chat) {
        this.chat = chat;
    }

    @GetMapping
    ChatService.Sessions list(Authentication auth, @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return chat.sessions(userId(auth), page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ChatService.Created create(Authentication auth, @Valid @RequestBody(required = false) Create request) {
        return chat.create(userId(auth), request == null ? null : request.title());
    }

    @DeleteMapping("/{sessionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(Authentication auth, @PathVariable @Positive long sessionId) {
        chat.delete(userId(auth), sessionId);
    }

    @GetMapping("/{sessionId}/messages")
    ChatService.History history(Authentication auth, @PathVariable @Positive long sessionId,
            @RequestParam(required = false) @Size(max = 19) String cursor) {
        return chat.history(userId(auth), sessionId, cursor);
    }

    @PostMapping("/{sessionId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    ChatService.Sent send(Authentication auth, @PathVariable @Positive long sessionId,
            @Valid @RequestBody Send request) {
        return chat.send(auth, userId(auth), sessionId, request);
    }

    private long userId(Authentication auth) {
        return Long.parseLong(auth.getName());
    }

    record Create(@Size(max = 100) String title) {}
    record Send(@NotBlank @Size(max = 2000) String content,
                @Size(max = 5) List<@NotNull @Positive Long> selectedDogIds,
                @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
                @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude) {}
}
