package com.advancedswiftly.pawfit.business.domain;

/** 이벤트·광고 신청의 처리 상태. */
public enum ApplicationStatus {
    RECEIVED,
    IN_REVIEW,
    NEEDS_INFO,
    APPROVED,
    REJECTED,
    CANCELED;

    /** 심사가 시작되기 전(RECEIVED)이거나 보완 요청(NEEDS_INFO) 상태일 때만 업체가 취소할 수 있다. */
    public boolean isCancelable() {
        return this == RECEIVED || this == NEEDS_INFO;
    }

    /** 현재 상태에서 심사 동작을 하면 도달하는 상태. 허용되지 않는 동작이면 null. */
    public ApplicationStatus afterReview(ReviewAction action) {
        return switch (action) {
            case START_REVIEW -> this == RECEIVED ? IN_REVIEW : null;
            case NEEDS_INFO -> this == IN_REVIEW ? NEEDS_INFO : null;
            case APPROVE -> this == IN_REVIEW ? APPROVED : null;
            case REJECT -> this == IN_REVIEW ? REJECTED : null;
        };
    }
}
