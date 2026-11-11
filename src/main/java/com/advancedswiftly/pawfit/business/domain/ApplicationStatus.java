package com.advancedswiftly.pawfit.business.domain;

/** 이벤트·광고 신청의 처리 상태. */
public enum ApplicationStatus {
    RECEIVED,
    IN_REVIEW,
    APPROVED,
    REJECTED,
    CANCELED;

    /** 심사가 시작되기 전(RECEIVED)에만 취소할 수 있다. */
    public boolean isCancelable() {
        return this == RECEIVED;
    }
}
