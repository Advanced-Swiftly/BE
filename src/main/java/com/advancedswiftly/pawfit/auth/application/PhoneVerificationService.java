package com.advancedswiftly.pawfit.auth.application;

import com.advancedswiftly.pawfit.auth.api.dto.AuthDtos.PhoneVerificationConfirmResponse;
import com.advancedswiftly.pawfit.auth.api.dto.AuthDtos.PhoneVerificationCreateResponse;
import com.advancedswiftly.pawfit.auth.domain.PhoneVerification;
import com.advancedswiftly.pawfit.auth.domain.PhoneVerificationPurpose;
import com.advancedswiftly.pawfit.auth.infrastructure.PhoneVerificationRepository;
import com.advancedswiftly.pawfit.global.config.PawfitProperties;
import com.advancedswiftly.pawfit.global.error.BusinessException;
import com.advancedswiftly.pawfit.global.error.ErrorCode;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PhoneVerificationService {

    private static final Base64.Encoder TOKEN_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private final PhoneVerificationRepository repository;
    private final PhoneVerificationSender sender;
    private final PhoneNumberNormalizer phoneNumberNormalizer;
    private final PasswordEncoder passwordEncoder;
    private final PawfitProperties.Auth properties;
    private final SecureRandom secureRandom;
    private final Clock clock;

    @Autowired
    public PhoneVerificationService(
            PhoneVerificationRepository repository,
            PhoneVerificationSender sender,
            PhoneNumberNormalizer phoneNumberNormalizer,
            PasswordEncoder passwordEncoder,
            PawfitProperties properties) {
        this(
                repository,
                sender,
                phoneNumberNormalizer,
                passwordEncoder,
                properties.getAuth(),
                new SecureRandom(),
                Clock.systemUTC());
    }

    PhoneVerificationService(
            PhoneVerificationRepository repository,
            PhoneVerificationSender sender,
            PhoneNumberNormalizer phoneNumberNormalizer,
            PasswordEncoder passwordEncoder,
            PawfitProperties.Auth properties,
            SecureRandom secureRandom,
            Clock clock) {
        this.repository = repository;
        this.sender = sender;
        this.phoneNumberNormalizer = phoneNumberNormalizer;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
        this.secureRandom = secureRandom;
        this.clock = clock;
    }

    @Transactional
    public PhoneVerificationCreateResponse send(
            String rawPhoneNumber, PhoneVerificationPurpose purpose) {
        String phoneNumber = phoneNumberNormalizer.normalize(rawPhoneNumber);
        Instant now = clock.instant();
        enforceRateLimit(phoneNumber, now);

        String code = verificationCode();
        PhoneVerification verification = repository.saveAndFlush(PhoneVerification.start(
                phoneNumber,
                purpose,
                passwordEncoder.encode(code),
                now.plus(properties.getVerificationCodeTtl())));
        sender.send(phoneNumber, code);

        return new PhoneVerificationCreateResponse(
                verification.getId(), properties.getVerificationCodeTtl().toSeconds());
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public PhoneVerificationConfirmResponse confirm(Long verificationId, String code) {
        Instant now = clock.instant();
        PhoneVerification verification = repository.findLockedById(verificationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_VERIFICATION_CODE));
        if (!verification.canVerifyAt(now, properties.getVerificationMaxAttempts())
                || !passwordEncoder.matches(code, verification.getCodeHash())) {
            verification.recordFailedAttempt();
            throw new BusinessException(ErrorCode.INVALID_VERIFICATION_CODE);
        }

        String token = randomToken();
        verification.verify(
                TokenHash.sha256(token),
                now,
                now.plus(properties.getVerificationTokenTtl()));
        return new PhoneVerificationConfirmResponse(true, token);
    }

    @Transactional
    public String consumeSignupToken(String verificationToken) {
        Instant now = clock.instant();
        PhoneVerification verification = repository
                .findByVerificationTokenHash(TokenHash.sha256(verificationToken))
                .filter(value -> value.canConsumeAt(now, PhoneVerificationPurpose.SIGN_UP))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_VERIFICATION_TOKEN));
        verification.consume(now);
        return verification.getPhoneNumber();
    }

    private void enforceRateLimit(String phoneNumber, Instant now) {
        long hourlyRequests = repository.countByPhoneNumberAndCreatedAtAfter(
                phoneNumber, now.minus(1, ChronoUnit.HOURS));
        boolean inCooldown = repository.findTopByPhoneNumberOrderByCreatedAtDesc(phoneNumber)
                .map(verification -> verification.getCreatedAt()
                        .plus(properties.getVerificationResendCooldown())
                        .isAfter(now))
                .orElse(false);
        if (hourlyRequests >= properties.getVerificationHourlyLimit() || inCooldown) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS);
        }
    }

    private String randomToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return TOKEN_ENCODER.encodeToString(bytes);
    }

    private String verificationCode() {
        if (properties.isVerificationDemoEnabled()) {
            String demoCode = properties.getVerificationDemoCode();
            if (demoCode == null || !demoCode.matches("^\\d{6}$")) {
                throw new IllegalStateException("시연용 인증번호는 6자리 숫자여야 합니다.");
            }
            return demoCode;
        }
        return "%06d".formatted(secureRandom.nextInt(1_000_000));
    }
}
