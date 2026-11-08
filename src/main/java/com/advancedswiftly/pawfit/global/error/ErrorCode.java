package com.advancedswiftly.pawfit.global.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "COMMON_001", "요청 값이 올바르지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH_001", "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "AUTH_002", "접근 권한이 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON_002", "요청한 리소스를 찾을 수 없습니다."),
    CONFLICT(HttpStatus.CONFLICT, "COMMON_003", "현재 상태에서는 요청을 처리할 수 없습니다."),
    DUPLICATE_BUSINESS_NUMBER(HttpStatus.BAD_REQUEST, "DUPLICATE_BUSINESS_NUMBER", "요청을 처리할 수 없습니다."),
    INVALID_FILE(HttpStatus.BAD_REQUEST, "FILE_001", "파일이 올바르지 않습니다."),
    FILE_TOO_LARGE(HttpStatus.CONTENT_TOO_LARGE, "FILE_002", "허용된 파일 크기를 초과했습니다."),
    UNSUPPORTED_FILE_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "FILE_003", "지원하지 않는 파일 형식입니다."),
    AI_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "AI_001", "AI 서비스를 일시적으로 사용할 수 없습니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_500", "서버 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
