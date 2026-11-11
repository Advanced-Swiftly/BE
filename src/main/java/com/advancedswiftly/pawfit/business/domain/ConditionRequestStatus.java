package com.advancedswiftly.pawfit.business.domain;

/** 입장 조건 변경 요청의 심사 상태. */
public enum ConditionRequestStatus {
    RECEIVED,
    IN_REVIEW,
    NEEDS_SUPPLEMENT,
    APPROVED,
    REJECTED,
    CANCELED
}
