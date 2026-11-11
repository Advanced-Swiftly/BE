package com.advancedswiftly.pawfit.global.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    CHAT_SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAT_SESSION_NOT_FOUND", "요청을 처리할 수 없습니다."),
    CHAT_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "CHAT_SERVICE_UNAVAILABLE", "요청을 처리할 수 없습니다."),
    CHAT_SESSION_CHANGED(HttpStatus.CONFLICT, "CHAT_SESSION_CHANGED", "대화가 갱신되었습니다. 다시 시도해 주세요."),
    SYNC_ALREADY_RUNNING(HttpStatus.CONFLICT, "SYNC_ALREADY_RUNNING", "요청을 처리할 수 없습니다."),
    PUBLIC_DATA_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "PUBLIC_DATA_UNAVAILABLE", "공공데이터 동기화를 시작할 수 없습니다."),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "INVALID_PASSWORD", "요청을 처리할 수 없습니다."),
    MEMBER_DUPLICATE_PHONE_NUMBER(HttpStatus.BAD_REQUEST, "DUPLICATE_PHONE_NUMBER", "요청을 처리할 수 없습니다."),
    PLACE_NOT_FOUND(HttpStatus.NOT_FOUND, "PLACE_NOT_FOUND", "요청을 처리할 수 없습니다."),
    DOG_NOT_OWNED(HttpStatus.FORBIDDEN, "DOG_NOT_OWNED", "요청을 처리할 수 없습니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "COMMON_001", "요청 값이 올바르지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH_001", "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "AUTH_002", "접근 권한이 없습니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "요청을 처리할 수 없습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "요청을 처리할 수 없습니다."),
    INVALID_VERIFICATION_CODE(HttpStatus.BAD_REQUEST, "INVALID_VERIFICATION_CODE", "요청을 처리할 수 없습니다."),
    INVALID_VERIFICATION_TOKEN(HttpStatus.BAD_REQUEST, "INVALID_VERIFICATION_TOKEN", "요청을 처리할 수 없습니다."),
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_REQUESTS", "요청을 처리할 수 없습니다."),
    REQUIRED_TERM_NOT_AGREED(HttpStatus.BAD_REQUEST, "REQUIRED_TERM_NOT_AGREED", "요청을 처리할 수 없습니다."),
    INVALID_ROLE(HttpStatus.BAD_REQUEST, "INVALID_ROLE", "유효하지 않은 가입 역할입니다."),
    DUPLICATE_PHONE_NUMBER(HttpStatus.CONFLICT, "DUPLICATE_PHONE_NUMBER", "이미 가입된 휴대전화 번호입니다."),
    PHONE_DELIVERY_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "PHONE_DELIVERY_UNAVAILABLE", "인증번호 발송 서비스를 일시적으로 사용할 수 없습니다."),
    DOG_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "DOG_LIMIT_EXCEEDED", "요청을 처리할 수 없습니다."),
    DOG_NOT_FOUND(HttpStatus.NOT_FOUND, "DOG_NOT_FOUND", "요청을 처리할 수 없습니다."),
    EVENT_NOT_FOUND(HttpStatus.NOT_FOUND, "EVENT_NOT_FOUND", "요청을 처리할 수 없습니다."),
    DRAW_ENTRY_NOT_FOUND(HttpStatus.NOT_FOUND, "DRAW_ENTRY_NOT_FOUND", "요청을 처리할 수 없습니다."),
    INVALID_OR_REVOKED_QR(HttpStatus.BAD_REQUEST, "INVALID_OR_REVOKED_QR", "요청을 처리할 수 없습니다."),
    INSUFFICIENT_STAMPS(HttpStatus.BAD_REQUEST, "INSUFFICIENT_STAMPS", "요청을 처리할 수 없습니다."),
    DRAW_ENTRY_ALREADY_EXISTS(HttpStatus.CONFLICT, "DRAW_ENTRY_ALREADY_EXISTS", "요청을 처리할 수 없습니다."),
    INQUIRY_NOT_FOUND(HttpStatus.NOT_FOUND, "INQUIRY_NOT_FOUND", "요청을 처리할 수 없습니다."),
    INVALID_DOCUMENT_FORMAT(HttpStatus.BAD_REQUEST, "INVALID_DOCUMENT_FORMAT", "요청을 처리할 수 없습니다."),
    RESUBMIT_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "RESUBMIT_NOT_ALLOWED", "요청을 처리할 수 없습니다."),
    RECORD_NOT_EDITABLE(HttpStatus.BAD_REQUEST, "RECORD_NOT_EDITABLE", "요청을 처리할 수 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON_002", "요청한 리소스를 찾을 수 없습니다."),
    CONFLICT(HttpStatus.CONFLICT, "COMMON_003", "현재 상태에서는 요청을 처리할 수 없습니다."),
    DUPLICATE_APPLICATION(HttpStatus.BAD_REQUEST, "DUPLICATE_APPLICATION", "요청을 처리할 수 없습니다."),
    CANCEL_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "CANCEL_NOT_ALLOWED", "요청을 처리할 수 없습니다."),
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
