package com.advancedswiftly.pawfit.business.domain;

/** 업체 QR의 사용 상태. */
public enum QrStatus {
    ACTIVE,
    DISABLED,
    /** 재발급으로 폐기된 QR. 다시 활성화할 수 없다. */
    REVOKED
}
