package com.advancedswiftly.pawfit.business.domain;

/** 사업자 인증 요청의 처리 상태. */
public enum VerificationStatus {
    RECEIVED,
    IN_REVIEW,
    NEEDS_SUPPLEMENT,
    APPROVED,
    REJECTED;

    /**
     * 운영자 심사 동작 뒤의 상태. 허용되지 않는 동작이면 null.
     * NEEDS_INFO 동작은 보완 요청(NEEDS_SUPPLEMENT) 상태가 된다.
     */
    public VerificationStatus afterReview(ReviewAction action) {
        return switch (action) {
            case START_REVIEW -> this == RECEIVED ? IN_REVIEW : null;
            case NEEDS_INFO -> this == IN_REVIEW ? NEEDS_SUPPLEMENT : null;
            case APPROVE -> this == IN_REVIEW ? APPROVED : null;
            case REJECT -> this == IN_REVIEW ? REJECTED : null;
        };
    }
}
