package com.advancedswiftly.pawfit.business.api;

import com.advancedswiftly.pawfit.business.domain.ApplicationStatus;
import com.advancedswiftly.pawfit.global.error.BusinessException;
import com.advancedswiftly.pawfit.global.error.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;

/** 업체 신청 컨트롤러들이 같이 쓰는 요청 처리 도우미. */
final class BusinessControllerSupport {

    private BusinessControllerSupport() {
    }

    /** 인증 필터가 Authentication의 name에 user_id를 담는다고 가정한다. */
    static Long currentUserId(Authentication authentication) {
        try {
            return Long.valueOf(authentication.getName());
        } catch (NumberFormatException exception) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }

    /** status 파라미터가 비어 있으면 필터 없음, 허용되지 않는 값이면 400으로 응답한다. */
    static ApplicationStatus parseStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        try {
            return ApplicationStatus.valueOf(status.trim());
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "status 값이 올바르지 않습니다. (RECEIVED, IN_REVIEW, APPROVED, REJECTED, CANCELED)");
        }
    }
}
