package com.advancedswiftly.pawfit.business.domain;

/** 사업자 인증 요청의 처리 상태. */
public enum VerificationStatus {
    RECEIVED,
    IN_REVIEW,
    NEEDS_SUPPLEMENT,
    APPROVED,
    REJECTED
}
