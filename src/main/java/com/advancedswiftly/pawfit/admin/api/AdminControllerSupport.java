package com.advancedswiftly.pawfit.admin.api;

import com.advancedswiftly.pawfit.global.error.BusinessException;
import com.advancedswiftly.pawfit.global.error.ErrorCode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;

/** 운영자 컨트롤러들이 같이 쓰는 요청 처리 도우미. */
final class AdminControllerSupport {

    /** 목록 API의 한 페이지 크기. */
    static final int PAGE_SIZE = 20;

    private AdminControllerSupport() {
    }

    /** 인증 필터가 Authentication의 name에 user_id를 담는다고 가정한다. */
    static Long currentUserId(Authentication authentication) {
        try {
            return Long.valueOf(authentication.getName());
        } catch (NumberFormatException exception) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }

    /** page가 비어 있으면 1, 숫자가 아니거나 1 미만이면 400. */
    static int parsePage(String page) {
        if (!StringUtils.hasText(page)) {
            return 1;
        }
        try {
            int value = Integer.parseInt(page.trim());
            if (value < 1) {
                throw new NumberFormatException();
            }
            return value;
        } catch (NumberFormatException exception) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "page는 1 이상의 숫자여야 합니다.");
        }
    }

    /** 선택 날짜 파라미터(yyyy-MM-dd). 비어 있으면 null, 형식이 틀리면 400. */
    static LocalDate optionalDate(String name, String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException exception) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, name + " 형식이 올바르지 않습니다. (yyyy-MM-dd)");
        }
    }

    /** 허용된 값이면 그대로, 비어 있으면 null, 아니면 400. */
    static <E extends Enum<E>> E optionalEnum(Class<E> type, String name, String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Enum.valueOf(type, value.trim());
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, name + " 값이 올바르지 않습니다.");
        }
    }
}
