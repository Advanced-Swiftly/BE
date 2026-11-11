package com.advancedswiftly.pawfit.business.domain;

/** 입장 조건 변경 요청의 심사 상태. */
public enum ConditionRequestStatus {
    RECEIVED,
    IN_REVIEW,
    NEEDS_SUPPLEMENT,
    APPROVED,
    REJECTED,
    CANCELED;

    /**
     * 운영자 심사 동작 뒤의 상태. 허용되지 않는 동작이면 null.
     * NEEDS_INFO 동작은 보완 요청(NEEDS_SUPPLEMENT) 상태가 된다.
     */
    public ConditionRequestStatus afterReview(ReviewAction action) {
        return switch (action) {
            case START_REVIEW -> this == RECEIVED ? IN_REVIEW : null;
            case NEEDS_INFO -> this == IN_REVIEW ? NEEDS_SUPPLEMENT : null;
            case APPROVE -> this == IN_REVIEW ? APPROVED : null;
            case REJECT -> this == IN_REVIEW ? REJECTED : null;
        };
    }
}
