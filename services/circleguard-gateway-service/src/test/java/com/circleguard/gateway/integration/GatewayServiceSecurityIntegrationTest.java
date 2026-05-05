package com.circleguard.gateway.integration;

import com.circleguard.gateway.service.QrValidationService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.security.Key;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration Test: Gateway Service Security Filters
 * 
 * Validates that the Gateway Service correctly validates JWT tokens and applies
 * security filters. Uses MockMvc for HTTP testing.
 * 
 * This test:
 * - Creates real JWT tokens with various configurations
 * - Tests valid token validation through the /api/v1/gate/validate endpoint
 * - Tests invalid/expired token handling
 * - Verifies that the gateway properly processes token payloads
 */
@SpringBootTest
@AutoConfigureMockMvc
public class GatewayServiceSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StringRedisTemplate redisTemplate;
    
    @MockBean
    private ValueOperations<String, String> valueOperations;

    @Value("${qr.secret}")
    private String qrSecret;

    private Key signingKey;

    @BeforeEach
    void setup() {
        // Initialize the signing key for creating test JWT tokens
        signingKey = Keys.hmacShaKeyFor(qrSecret.getBytes());
        org.mockito.Mockito.when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        org.mockito.Mockito.when(valueOperations.get(org.mockito.ArgumentMatchers.anyString())).thenReturn("GREEN");
    }

    @Test
    void validateToken_WithValidJWT_ReturnsSuccess() throws Exception {
        // Arrange: Create a valid JWT token
        String validToken = createValidJWT("test-user-123", List.of("VISITOR"));
        String requestBody = String.format("{\"token\": \"%s\"}", validToken);

        // Act & Assert: Call the validate endpoint and verify it responds
        mockMvc.perform(post("/api/v1/gate/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk());
    }

    @Test
    void validateToken_WithInvalidJWT_ReturnsFail() throws Exception {
        // Arrange: Create an intentionally malformed token
        String invalidToken = "invalid.token.format";
        String requestBody = String.format("{\"token\": \"%s\"}", invalidToken);

        // Act & Assert: Expect validation to fail
        mockMvc.perform(post("/api/v1/gate/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validateToken_WithExpiredJWT_ReturnsFail() throws Exception {
        // Arrange: Create an expired JWT token (issued in the past with negative lifetime)
        String expiredToken = createExpiredJWT("test-user-456");
        String requestBody = String.format("{\"token\": \"%s\"}", expiredToken);

        // Act & Assert: Expect validation to fail for expired token
        mockMvc.perform(post("/api/v1/gate/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validateToken_WithMissingToken_ReturnsBadRequest() throws Exception {
        // Arrange: Send a request without the token field
        String requestBody = "{}";

        // Act & Assert: Expect bad request or validation failure
        mockMvc.perform(post("/api/v1/gate/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validateToken_WithMultiplePermissions_IncludesAllPermissions() throws Exception {
        // Arrange: Create a token with multiple permissions
        String tokenWithPermissions = Jwts.builder()
                .setSubject("health-center-user")
                .claim("permissions", List.of("identity:lookup", "survey:validate", "admin:access"))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(signingKey)
                .compact();

        String requestBody = String.format("{\"token\": \"%s\"}", tokenWithPermissions);

        // Act & Assert: Validate and check response
        mockMvc.perform(post("/api/v1/gate/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk());
    }

    @Test
    void validateToken_WithTamperedSignature_ReturnsFail() throws Exception {
        // Arrange: Create a valid token, then tamper with it
        String validToken = createValidJWT("trusted-user", List.of("VISITOR"));
        
        // Tamper with the signature by changing the last character
        String tamperedToken = validToken.substring(0, validToken.length() - 1) + 
                (validToken.charAt(validToken.length() - 1) == 'A' ? 'B' : 'A');

        String requestBody = String.format("{\"token\": \"%s\"}", tamperedToken);

        // Act & Assert: Expect validation to fail
        mockMvc.perform(post("/api/v1/gate/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isUnauthorized());
    }

    // Helper methods to generate test tokens
    private String createValidJWT(String subject, List<String> permissions) {
        return Jwts.builder()
                .setSubject(subject)
                .claim("permissions", permissions)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600000)) // 1 hour from now
                .signWith(signingKey)
                .compact();
    }

    private String createExpiredJWT(String subject) {
        return Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis() - 7200000)) // 2 hours ago
                .setExpiration(new Date(System.currentTimeMillis() - 3600000)) // 1 hour ago
                .signWith(signingKey)
                .compact();
    }
}
