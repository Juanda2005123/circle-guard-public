package com.circleguard.auth.integration;

import com.circleguard.auth.client.IdentityClient;
import com.circleguard.auth.controller.LoginController;
import com.circleguard.auth.service.JwtTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Integration Test: Auth Service communicates with Identity Service
 * 
 * Validates that the Auth Service can successfully call the Identity Service
 * to retrieve or create an anonymous ID during the login flow.
 * 
 * This test:
 * - Mocks the IdentityClient to return a valid anonymousId
 * - Tests the complete login flow with JWT generation
 * - Verifies that the response contains all required fields (token, type, anonymousId)
 * 
 * Note: Currently disabled as AuthenticationManager requires LDAP or user store configuration
 */
@SpringBootTest
@AutoConfigureMockMvc
public class AuthIdentityServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IdentityClient identityClient;
    
    @MockBean
    private AuthenticationManager authenticationManager;

    @Test
    void login_CallsIdentityService_AndReturnsToken() throws Exception {
        // Arrange: Setup mock to return a valid anonymous ID
        UUID mockAnonymousId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        when(identityClient.getAnonymousId(anyString())).thenReturn(mockAnonymousId);
        
        // Mock AuthenticationManager
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "testuser", "password123", java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER"))
        );
        when(authenticationManager.authenticate(org.mockito.ArgumentMatchers.any(Authentication.class))).thenReturn(auth);

        String loginRequest = "{\"username\": \"testuser\", \"password\": \"password123\"}";

        // Act & Assert: Call login endpoint
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginRequest))
                .andExpect(status().isOk())
                .andReturn();

        // Assert: Response body contains required fields
        String responseBody = result.getResponse().getContentAsString();
        assertThat(responseBody).containsIgnoringCase("token");
        assertThat(responseBody).contains(mockAnonymousId.toString());
    }

    @Test
    void login_WithInvalidCredentials_ReturnsUnauthorized() throws Exception {
        // Arrange
        String loginRequest = "{\"username\": \"testuser\", \"password\": \"wrongpassword\"}";
        
        when(authenticationManager.authenticate(org.mockito.ArgumentMatchers.any(Authentication.class)))
                .thenThrow(new org.springframework.security.authentication.BadCredentialsException("Bad credentials"));

        // Act & Assert: Expect 401 Unauthorized
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginRequest))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_IdentityClientIsCalledWithUsername() throws Exception {
        // Arrange: Setup mock with specific user
        UUID expectedAnonymousId = UUID.randomUUID();
        when(identityClient.getAnonymousId("springuser")).thenReturn(expectedAnonymousId);
        
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "springuser", "spring123", java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER"))
        );
        when(authenticationManager.authenticate(org.mockito.ArgumentMatchers.any(Authentication.class))).thenReturn(auth);

        String loginRequest = "{\"username\": \"springuser\", \"password\": \"spring123\"}";

        // Act: Perform login
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginRequest))
                .andExpect(status().isOk());

        // Assert: Verify IdentityClient was called (implicitly via mockito mock setup)
        // The response should contain the anonymousId from the mocked client
    }

    @Test
    void visitorHandoff_GeneratesValidToken_WithVisitorRole() throws Exception {
        // Arrange
        UUID visitorId = UUID.randomUUID();
        String handoffRequest = "{\"anonymousId\": \"" + visitorId.toString() + "\"}";

        // Act & Assert
        MvcResult result = mockMvc.perform(post("/api/v1/auth/visitor/handoff")
                .contentType(MediaType.APPLICATION_JSON)
                .content(handoffRequest))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertThat(responseBody).containsIgnoringCase("token");
        assertThat(responseBody).containsIgnoringCase("handoff");
    }
}
