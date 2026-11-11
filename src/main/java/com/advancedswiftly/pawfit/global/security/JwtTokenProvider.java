package com.advancedswiftly.pawfit.global.security;

import com.advancedswiftly.pawfit.auth.domain.User;
import com.advancedswiftly.pawfit.auth.domain.UserRole;
import com.advancedswiftly.pawfit.global.config.PawfitProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class JwtTokenProvider {

    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final ObjectMapper objectMapper;
    private final PawfitProperties.Auth properties;
    private final Clock clock;

    @Autowired
    public JwtTokenProvider(ObjectMapper objectMapper, PawfitProperties properties) {
        this(objectMapper, properties.getAuth(), Clock.systemUTC());
    }

    JwtTokenProvider(
            ObjectMapper objectMapper, PawfitProperties.Auth properties, Clock clock) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.clock = clock;
        if (properties.getJwtSecret().getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("AUTH_JWT_SECRET은 32 byte 이상이어야 합니다.");
        }
    }

    public String createAccessToken(User user) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(properties.getAccessTokenTtl());
        try {
            String header = encodeJson(Map.of("alg", "HS256", "typ", "JWT"));
            String payload = encodeJson(Map.of(
                    "sub", user.getId().toString(),
                    "role", user.getRole().name(),
                    "type", "ACCESS",
                    "iat", issuedAt.getEpochSecond(),
                    "exp", expiresAt.getEpochSecond()));
            String unsignedToken = header + "." + payload;
            return unsignedToken + "." + sign(unsignedToken);
        } catch (Exception exception) {
            throw new IllegalStateException("액세스 토큰을 생성할 수 없습니다.", exception);
        }
    }

    public AccessTokenClaims parseAccessToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new IllegalArgumentException("JWT 형식이 올바르지 않습니다.");
            }
            String unsignedToken = parts[0] + "." + parts[1];
            if (!MessageDigest.isEqual(
                    URL_DECODER.decode(parts[2]), URL_DECODER.decode(sign(unsignedToken)))) {
                throw new IllegalArgumentException("JWT 서명이 올바르지 않습니다.");
            }

            JsonNode payload = objectMapper.readTree(URL_DECODER.decode(parts[1]));
            if (!"ACCESS".equals(payload.path("type").asText())) {
                throw new IllegalArgumentException("액세스 토큰이 아닙니다.");
            }
            Instant expiresAt = Instant.ofEpochSecond(payload.path("exp").asLong());
            if (!expiresAt.isAfter(clock.instant())) {
                throw new IllegalArgumentException("만료된 액세스 토큰입니다.");
            }
            return new AccessTokenClaims(
                    Long.valueOf(payload.path("sub").asText()),
                    UserRole.valueOf(payload.path("role").asText()),
                    expiresAt);
        } catch (Exception exception) {
            throw new IllegalArgumentException("유효하지 않은 액세스 토큰입니다.", exception);
        }
    }

    private String encodeJson(Map<String, Object> value) throws Exception {
        return URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(value));
    }

    private String sign(String value) throws Exception {
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        mac.init(new SecretKeySpec(
                properties.getJwtSecret().getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
        return URL_ENCODER.encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    }

    public record AccessTokenClaims(Long userId, UserRole role, Instant expiresAt) {
    }
}
