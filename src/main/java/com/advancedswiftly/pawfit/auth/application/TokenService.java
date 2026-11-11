package com.advancedswiftly.pawfit.auth.application;

import com.advancedswiftly.pawfit.auth.domain.RefreshToken;
import com.advancedswiftly.pawfit.auth.domain.User;
import com.advancedswiftly.pawfit.auth.infrastructure.RefreshTokenRepository;
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

    @Autowired
    public TokenService(
            RefreshTokenRepository refreshTokenRepository,
            JwtTokenProvider jwtTokenProvider,
            PawfitProperties properties) {
        this(
                refreshTokenRepository,
                jwtTokenProvider,
                properties.getAuth(),
                new SecureRandom(),
                Clock.systemUTC());
    }

    TokenService(
            RefreshTokenRepository refreshTokenRepository,
            JwtTokenProvider jwtTokenProvider,
            PawfitProperties.Auth properties,
            SecureRandom secureRandom,
            Clock clock) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.properties = properties;
        this.secureRandom = secureRandom;
        this.clock = clock;
    }

    @Transactional
    public IssuedTokens issue(User user, String deviceToken) {
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
        return refreshTokenRepository
                .findForUpdateByTokenHash(TokenHash.sha256(rawRefreshToken))
                .filter(token -> token.isUsableAt(now))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));
    }

    private String randomToken() {
        byte[] bytes = new byte[48];
        secureRandom.nextBytes(bytes);
        return TOKEN_ENCODER.encodeToString(bytes);
    }

    public record IssuedTokens(String accessToken, String refreshToken) {
    }
}
