package com.advancedswiftly.pawfit.admin.api.dto;

/** 신청 심사 요청의 동작. 도메인의 ReviewAction과 같은 값이다. */
public enum ApplicationReviewAction {
    START_REVIEW,
    NEEDS_INFO,
    APPROVE,
    REJECT
}
