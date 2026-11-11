package com.advancedswiftly.pawfit.admin.api.dto;

/** 당첨·배송 처리 동작. */
public enum DrawEntryAction {
    CANCEL_WIN,
    CONTACTED,
    ADDRESS_CONFIRMED,
    SHIPPED,
    DELIVERED
}
