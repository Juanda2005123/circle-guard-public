package com.circleguard.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class QrTokenServiceTest {

    private QrTokenService qrTokenService;
    private final String testSecret = "my-qr-secret-key-for-dev-1234567890-extra";

    @BeforeEach
    void setUp() {
        qrTokenService = new QrTokenService(testSecret, 60000);
    }

    @Test
    void testGenerateQrToken_ShouldReturnValidJwt() {
        UUID anonymousId = UUID.randomUUID();
        String token = qrTokenService.generateQrToken(anonymousId);

        assertNotNull(token);

        Claims claims = Jwts.parserBuilder()
                .setSigningKey(testSecret.getBytes())
                .build()
                .parseClaimsJws(token)
                .getBody();

        assertEquals(anonymousId.toString(), claims.getSubject());
        assertNotNull(claims.getExpiration());
        assertNotNull(claims.getIssuedAt());
    }
}
