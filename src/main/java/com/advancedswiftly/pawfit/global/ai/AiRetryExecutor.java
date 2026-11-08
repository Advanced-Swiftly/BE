package com.advancedswiftly.pawfit.global.ai;

import java.time.Duration;
import java.util.concurrent.Callable;

final class AiRetryExecutor {

    private final int maxRetries;
    private final Duration retryDelay;

    AiRetryExecutor(int maxRetries, Duration retryDelay) {
        if (maxRetries < 0) {
            throw new IllegalArgumentException("maxRetries must not be negative");
        }
        this.maxRetries = maxRetries;
        this.retryDelay = retryDelay;
    }

    <T> T execute(Callable<T> action) {
        int attempt = 0;
        while (true) {
            try {
                return action.call();
            } catch (AiClientException exception) {
                if (!exception.isRetryable() || attempt >= maxRetries) {
                    throw exception;
                }
                attempt++;
                waitBeforeRetry(attempt);
            } catch (Exception exception) {
                throw new AiClientException("AI 요청 처리 중 오류가 발생했습니다.", exception, false);
            }
        }
    }

    private void waitBeforeRetry(int attempt) {
        long delayMillis = retryDelay.toMillis() * (1L << Math.min(attempt - 1, 10));
        if (delayMillis <= 0) {
            return;
        }
        try {
            Thread.sleep(delayMillis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AiClientException("AI 재시도 대기가 중단되었습니다.", exception, false);
        }
    }
}
