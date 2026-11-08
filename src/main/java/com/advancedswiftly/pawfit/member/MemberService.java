package com.advancedswiftly.pawfit.member;

import com.advancedswiftly.pawfit.auth.application.PhoneVerificationService;
import com.advancedswiftly.pawfit.auth.domain.User;
import com.advancedswiftly.pawfit.auth.infrastructure.UserRepository;
import com.advancedswiftly.pawfit.global.error.BusinessException;
import com.advancedswiftly.pawfit.global.error.ErrorCode;
import com.advancedswiftly.pawfit.member.MemberController.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class MemberService {
    private final UserRepository users;
    private final PhoneVerificationService phoneVerification;
    private final PasswordEncoder passwords;

    MemberService(UserRepository users, PhoneVerificationService phoneVerification, PasswordEncoder passwords) {
        this.users = users;
        this.phoneVerification = phoneVerification;
        this.passwords = passwords;
    }

    public Profile profile(long id) {
        User user = activeUser(id, false);
        return new Profile(user.getId(), user.getRole(), user.getName(), user.getPhoneNumber());
    }

    @Transactional
    public UpdatedProfile updateProfile(long id, ProfileUpdate request) {
        User user = activeUser(id, true);
        String phone = null;
        if (request.phoneVerificationToken() != null) {
            phone = phoneVerification.consumeChangePhoneToken(request.phoneVerificationToken());
            if (!phone.equals(user.getPhoneNumber()) && users.existsByPhoneNumber(phone)) {
                throw new BusinessException(ErrorCode.MEMBER_DUPLICATE_PHONE_NUMBER);
            }
        }
        user.updateProfile(request.name(), phone);
        try {
            users.flush();
        } catch (DataIntegrityViolationException exception) {
            // 연락처 고유 제약으로 동시 변경에서의 중복도 차단합니다.
            throw new BusinessException(ErrorCode.MEMBER_DUPLICATE_PHONE_NUMBER);
        }
        return new UpdatedProfile(user.getId(), user.getName(), user.getPhoneNumber());
    }

    public Notifications notifications(long id) {
        return toNotifications(activeUser(id, false));
    }

    @Transactional
    public Notifications updateNotifications(long id, Notifications request) {
        User user = activeUser(id, true);
        user.updateNotifications(request.event(), request.inquiry(), request.reviewResult(), request.marketing());
        return toNotifications(user);
    }

    @Transactional
    public void withdraw(long id, Withdrawal request) {
        User user = activeUser(id, true);
        if (!passwords.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }
        user.withdraw(request.reason());
    }

    private User activeUser(long id, boolean lock) {
        return (lock ? users.findByIdForUpdate(id) : users.findById(id))
                .filter(User::isActive).orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }

    private Notifications toNotifications(User user) {
        return new Notifications(user.isNotifyEvent(), user.isNotifyInquiry(),
                user.isNotifyReviewResult(), user.isNotifyMarketing());
    }
}
