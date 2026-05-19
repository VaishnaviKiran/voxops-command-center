package com.voxops.incident;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "voxops.security")
public record SecurityProperties(
        String jwtSecret,
        long tokenTtlMinutes,
        String internalServiceToken
) {
    public String activeJwtSecret() {
        return jwtSecret == null || jwtSecret.isBlank()
                ? "local-dev-secret-change-me-local-dev-secret"
                : jwtSecret;
    }

    public long activeTokenTtlMinutes() {
        return tokenTtlMinutes <= 0 ? 480 : tokenTtlMinutes;
    }

    public String activeInternalServiceToken() {
        return internalServiceToken == null || internalServiceToken.isBlank()
                ? "local-dev-internal-token"
                : internalServiceToken;
    }
}
