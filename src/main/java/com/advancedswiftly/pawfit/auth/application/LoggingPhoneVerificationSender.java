package com.advancedswiftly.pawfit.auth.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** 실제 SMS provider 연결 전 local/test 환경에서만 사용하는 개발용 sender다. */
@Profile("!prod")
@Component
public class LoggingPhoneVerificationSender implements PhoneVerificationSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingPhoneVerificationSender.class);

    @Override
    public void send(String phoneNumber, String code) {
        log.debug("개발용 휴대전화 인증번호 발송: phone={}, code={}", mask(phoneNumber), code);
    }

    private String mask(String phoneNumber) {
        return phoneNumber.substring(0, 3) + "****" + phoneNumber.substring(phoneNumber.length() - 4);
    }
}
