package com.voxops.incident;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class JwtService {

    private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder BASE64_URL_DECODER = Base64.getUrlDecoder();

    private final SecurityProperties securityProperties;
    private final ObjectMapper objectMapper;

    public JwtService(SecurityProperties securityProperties, ObjectMapper objectMapper) {
        this.securityProperties = securityProperties;
        this.objectMapper = objectMapper;
    }

    public String createToken(AuthUser user) {
        try {
            Instant now = Instant.now();
            Instant expiresAt = now.plusSeconds(securityProperties.activeTokenTtlMinutes() * 60);

            Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("sub", user.email());
            payload.put("name", user.name());
            payload.put("role", user.role().name());
            payload.put("iat", now.getEpochSecond());
            payload.put("exp", expiresAt.getEpochSecond());

            String encodedHeader = encodeJson(header);
            String encodedPayload = encodeJson(payload);
            String unsignedToken = encodedHeader + "." + encodedPayload;
            return unsignedToken + "." + sign(unsignedToken);
        } catch (Exception error) {
            throw new IllegalStateException("Failed to create JWT", error);
        }
    }

    public Optional<AuthUser> validateToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return Optional.empty();
            }

            String unsignedToken = parts[0] + "." + parts[1];
            if (!constantTimeEquals(sign(unsignedToken), parts[2])) {
                return Optional.empty();
            }

            Map<String, Object> claims = objectMapper.readValue(
                    BASE64_URL_DECODER.decode(parts[1]),
                    new TypeReference<>() {
                    }
            );

            long expiresAt = ((Number) claims.get("exp")).longValue();
            if (Instant.now().getEpochSecond() >= expiresAt) {
                return Optional.empty();
            }

            return Optional.of(new AuthUser(
                    String.valueOf(claims.get("sub")),
                    String.valueOf(claims.get("name")),
                    AuthRole.valueOf(String.valueOf(claims.get("role")))
            ));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    public long expiresInSeconds() {
        return securityProperties.activeTokenTtlMinutes() * 60;
    }

    private String encodeJson(Map<String, Object> value) throws Exception {
        return BASE64_URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(value));
    }

    private String sign(String value) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(
                securityProperties.activeJwtSecret().getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        ));
        return BASE64_URL_ENCODER.encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    }

    private boolean constantTimeEquals(String left, String right) {
        return MessageDigest.isEqual(left.getBytes(StandardCharsets.UTF_8), right.getBytes(StandardCharsets.UTF_8));
    }
}
