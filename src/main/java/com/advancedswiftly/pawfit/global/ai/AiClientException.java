package com.advancedswiftly.pawfit.global.ai;

public class AiClientException extends RuntimeException {

    private final boolean retryable;

    public AiClientException(String message, boolean retryable) {
        super(message);
        this.retryable = retryable;
    }

    public AiClientException(String message, Throwable cause, boolean retryable) {
        super(message, cause);
        this.retryable = retryable;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
