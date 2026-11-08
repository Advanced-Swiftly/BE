package com.advancedswiftly.pawfit.auth.application;

import com.advancedswiftly.pawfit.global.error.BusinessException;
import com.advancedswiftly.pawfit.global.error.ErrorCode;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** SMS provider가 설정되지 않은 production에서 성공 응답을 가장하지 않는다. */
@Profile("prod")
@Component
public class UnavailablePhoneVerificationSender implements PhoneVerificationSender {

    @Override
    public void send(String phoneNumber, String code) {
        throw new BusinessException(ErrorCode.PHONE_DELIVERY_UNAVAILABLE);
    }
}
