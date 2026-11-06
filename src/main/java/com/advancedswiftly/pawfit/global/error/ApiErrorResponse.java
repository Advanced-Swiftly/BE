package com.advancedswiftly.pawfit.global.error;

import java.time.Instant;
import java.util.Map;

public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path,
        String requestId,
        Map<String, String> fieldErrors) {

    public static ApiErrorResponse of(
            ErrorCode errorCode,
            String message,
            String path,
            String requestId,
            Map<String, String> fieldErrors) {
        return new ApiErrorResponse(
                Instant.now(),
                errorCode.getStatus().value(),
                errorCode.getCode(),
                message,
                path,
                requestId,
                fieldErrors);
    }
}
