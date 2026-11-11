package com.advancedswiftly.pawfit.auth.application;

public interface PhoneVerificationSender {

    void send(String phoneNumber, String code);
}
