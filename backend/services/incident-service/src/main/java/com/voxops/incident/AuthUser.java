package com.voxops.incident;

public record AuthUser(
        String email,
        String name,
        AuthRole role
) {
}
