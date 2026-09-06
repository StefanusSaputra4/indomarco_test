package com.indomaret.backend.service;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.indomaret.backend.config.AppProperties;
import com.indomaret.backend.dto.LoginRequest;
import com.indomaret.backend.dto.LoginResponse;
import com.indomaret.backend.security.JwtUtil;
import com.indomaret.backend.service.impl.AuthServiceImpl;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private AppProperties appProperties;

    @InjectMocks
    private AuthServiceImpl authService;

    private AppProperties.Jwt jwtConfig;

    @BeforeEach
    void setUp() {
        jwtConfig = new AppProperties.Jwt();
        jwtConfig.setExpirationMs(3600000L);
    }

    @Test
    @DisplayName("Login: valid credentials returns JWT token and user info")
    void testLogin_success() {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("password123");

        Authentication authResult = new UsernamePasswordAuthenticationToken(
                "admin", "password123", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authResult);
        when(jwtUtil.generateToken("admin", "ADMIN")).thenReturn("mock-jwt-token");
        when(appProperties.getJwt()).thenReturn(jwtConfig);

        LoginResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("mock-jwt-token", response.getToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals("admin", response.getUsername());
        assertEquals("ADMIN", response.getRole());
        assertEquals(3600000L, response.getExpiresInMs());
    }

    @Test
    @DisplayName("Login: bad credentials throws BadCredentialsException")
    void testLogin_badCredentials() {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("wrongpassword");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }
}
