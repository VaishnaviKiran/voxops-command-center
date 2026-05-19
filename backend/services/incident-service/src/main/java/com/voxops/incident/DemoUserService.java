package com.voxops.incident;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
public class DemoUserService {

    private final Map<String, DemoUser> usersByEmail;

    public DemoUserService(PasswordEncoder passwordEncoder) {
        this.usersByEmail = Map.of(
                "admin@voxops.dev", new DemoUser(
                        new AuthUser("admin@voxops.dev", "VoxOps Admin", AuthRole.ADMIN),
                        passwordEncoder.encode("admin123")
                ),
                "responder@voxops.dev", new DemoUser(
                        new AuthUser("responder@voxops.dev", "Demo Responder", AuthRole.RESPONDER),
                        passwordEncoder.encode("responder123")
                ),
                "viewer@voxops.dev", new DemoUser(
                        new AuthUser("viewer@voxops.dev", "Demo Viewer", AuthRole.VIEWER),
                        passwordEncoder.encode("viewer123")
                )
        );
    }

    public Optional<DemoUser> findByEmail(String email) {
        return Optional.ofNullable(usersByEmail.get(email.toLowerCase()));
    }

    public record DemoUser(AuthUser user, String passwordHash) {
    }
}
