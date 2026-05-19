package com.voxops.incident;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final SecurityProperties securityProperties;

    public JwtAuthenticationFilter(JwtService jwtService, SecurityProperties securityProperties) {
        this.jwtService = jwtService;
        this.securityProperties = securityProperties;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        authenticateInternalService(request);
        authenticateBearerToken(request);
        filterChain.doFilter(request, response);
    }

    private void authenticateInternalService(HttpServletRequest request) {
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            return;
        }

        String internalToken = request.getHeader("X-Internal-Service-Token");
        if (internalToken == null || !internalToken.equals(securityProperties.activeInternalServiceToken())) {
            return;
        }

        setAuthentication(new AuthUser("service@voxops.internal", "Internal Service", AuthRole.SERVICE));
    }

    private void authenticateBearerToken(HttpServletRequest request) {
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            return;
        }

        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return;
        }

        jwtService.validateToken(authorization.substring("Bearer ".length()))
                .ifPresent(this::setAuthentication);
    }

    private void setAuthentication(AuthUser user) {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                user.email(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.role().name()))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
