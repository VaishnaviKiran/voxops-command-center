package com.voxops.incident;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final DemoUserService demoUserService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(DemoUserService demoUserService, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.demoUserService = demoUserService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {
        DemoUserService.DemoUser demoUser = demoUserService.findByEmail(request.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), demoUser.passwordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        return new LoginResponse(
                jwtService.createToken(demoUser.user()),
                "Bearer",
                jwtService.expiresInSeconds(),
                demoUser.user()
        );
    }
}
