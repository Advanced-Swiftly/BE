package com.advancedswiftly.pawfit.auth.application;

import com.advancedswiftly.pawfit.global.error.BusinessException;
import com.advancedswiftly.pawfit.global.error.ErrorCode;
import org.springframework.stereotype.Component;

@Component
public class PhoneNumberNormalizer {

    public String normalize(String rawPhoneNumber) {
        String compact = rawPhoneNumber.replace("-", "").replace(" ", "");
        if (compact.startsWith("+82")) {
            compact = "0" + compact.substring(3);
        }
        if (!compact.matches("^010\\d{8}$")) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "올바른 휴대전화 번호 형식이어야 합니다.");
        }
        return compact;
    }
}
