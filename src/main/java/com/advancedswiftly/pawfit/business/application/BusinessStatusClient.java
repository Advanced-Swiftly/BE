package com.advancedswiftly.pawfit.business.application;

/** 사업자등록번호에 해당하는 사업자가 계속사업자인지 조회한다. */
public interface BusinessStatusClient {
    boolean isActive(String businessNumber);
}
