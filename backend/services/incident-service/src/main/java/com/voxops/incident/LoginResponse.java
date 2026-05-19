package com.voxops.incident;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        AuthUser user
) {
}
