package com.indomaret.backend.service.impl;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import com.indomaret.backend.config.AppProperties;
import com.indomaret.backend.dto.LoginRequest;
import com.indomaret.backend.dto.LoginResponse;
import com.indomaret.backend.security.JwtUtil;
import com.indomaret.backend.service.AuthService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final AppProperties appProperties;

    @Override
    public LoginResponse login(LoginRequest request) {
        // Validasi username dan password menggunakan AuthenticationManager Spring Security
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        String username = authentication.getName();
        String role = authentication.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .map(r -> r.replace("ROLE_", ""))
                .orElse("STAFF");

        // Generate JWT Token
        String token = jwtUtil.generateToken(username, role);

        return LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .username(username)
                .role(role)
                .expiresInMs(appProperties.getJwt().getExpirationMs())
                .build();
    }
}
