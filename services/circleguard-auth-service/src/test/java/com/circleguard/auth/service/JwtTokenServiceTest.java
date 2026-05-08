package com.circleguard.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtTokenServiceTest {

    private JwtTokenService jwtTokenService;
    private final String testSecret = "my-super-secret-dev-key-32-chars-long-12345678";
    private final long testExpiration = 3600000;

    @BeforeEach
    void setUp() {
        jwtTokenService = new JwtTokenService(testSecret, testExpiration);
    }

    @Test
    void testGenerateToken_ShouldReturnValidJwt() {
        UUID anonymousId = UUID.randomUUID();
        Authentication auth = mock(Authentication.class);
        
        Collection<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_USER"),
                new SimpleGrantedAuthority("post:create")
        );
        doReturn(authorities).when(auth).getAuthorities();

        String token = jwtTokenService.generateToken(anonymousId, auth);

        assertNotNull(token);

        Claims claims = Jwts.parserBuilder()
                .setSigningKey(testSecret.getBytes())
                .build()
                .parseClaimsJws(token)
                .getBody();

        assertEquals(anonymousId.toString(), claims.getSubject());
        List<String> permissions = claims.get("permissions", List.class);
        assertTrue(permissions.contains("ROLE_USER"));
        assertTrue(permissions.contains("post:create"));
    }
}
