package com.advancedswiftly.pawfit.auth.application;

import com.advancedswiftly.pawfit.auth.domain.RefreshToken;
import com.advancedswiftly.pawfit.auth.domain.User;
import com.advancedswiftly.pawfit.auth.infrastructure.RefreshTokenRepository;
import com.advancedswiftly.pawfit.auth.infrastructure.UserRepository;
import com.advancedswiftly.pawfit.global.config.PawfitProperties;
import com.advancedswiftly.pawfit.global.error.BusinessException;
import com.advancedswiftly.pawfit.global.error.ErrorCode;
import com.advancedswiftly.pawfit.global.security.JwtTokenProvider;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TokenService {

    private static final Base64.Encoder TOKEN_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PawfitProperties.Auth properties;
    private final SecureRandom secureRandom;
    private final Clock clock;
    private final UserRepository users;

    @Autowired
    public TokenService(
            RefreshTokenRepository refreshTokenRepository,
            JwtTokenProvider jwtTokenProvider,
            PawfitProperties properties,
            UserRepository users) {
        this(
                refreshTokenRepository,
                jwtTokenProvider,
                properties.getAuth(),
                new SecureRandom(),
                Clock.systemUTC(),
                users);
    }

    TokenService(
            RefreshTokenRepository refreshTokenRepository,
            JwtTokenProvider jwtTokenProvider,
            PawfitProperties.Auth properties,
            SecureRandom secureRandom,
            Clock clock,
            UserRepository users) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.properties = properties;
        this.secureRandom = secureRandom;
        this.clock = clock;
        this.users = users;
    }

    @Transactional
    public IssuedTokens issue(User user, String deviceToken) {
        users.findByIdForUpdate(user.getId())
                .filter(User::isActive)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        String rawRefreshToken = randomToken();
        refreshTokenRepository.save(RefreshToken.issue(
                user,
                TokenHash.sha256(rawRefreshToken),
                deviceToken,
                clock.instant().plus(properties.getRefreshTokenTtl())));
        return new IssuedTokens(jwtTokenProvider.createAccessToken(user), rawRefreshToken);
    }

    @Transactional
    public IssuedTokens rotate(String rawRefreshToken) {
        Instant now = clock.instant();
        RefreshToken storedToken = findUsable(rawRefreshToken, now);
        storedToken.revoke(now);
        return issue(storedToken.getUser(), storedToken.getDeviceToken());
    }

    @Transactional
    public void revoke(Long userId, String rawRefreshToken, String deviceToken) {
        Instant now = clock.instant();
        RefreshToken storedToken = findUsable(rawRefreshToken, now);
        if (!storedToken.getUser().getId().equals(userId)
                || (deviceToken != null
                        && !deviceToken.isBlank()
                        && !Objects.equals(deviceToken, storedToken.getDeviceToken()))) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        storedToken.revoke(now);
    }

    private RefreshToken findUsable(String rawRefreshToken, Instant now) {
        // 비밀번호 변경과 token 회전 모두 user -> refresh token 순서로 잠근다.
        Long userId = refreshTokenRepository.findUserIdByTokenHash(TokenHash.sha256(rawRefreshToken))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));
        users.findByIdForUpdate(userId)
                .filter(User::isActive)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));
        return refreshTokenRepository
                .findForUpdateByTokenHash(TokenHash.sha256(rawRefreshToken))
                .filter(token -> token.isUsableAt(now))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));
    }

    @Transactional
    public void revokeAll(Long userId) {
        users.findByIdForUpdate(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        Instant now = clock.instant();
        refreshTokenRepository.findByUserIdAndRevokedAtIsNull(userId).forEach(token -> token.revoke(now));
    }

    private String randomToken() {
        byte[] bytes = new byte[48];
        secureRandom.nextBytes(bytes);
        return TOKEN_ENCODER.encodeToString(bytes);
    }

    public record IssuedTokens(String accessToken, String refreshToken) {
    }
}
