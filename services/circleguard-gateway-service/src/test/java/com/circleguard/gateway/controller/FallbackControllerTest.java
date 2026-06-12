package com.circleguard.gateway.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FallbackController.class)
public class FallbackControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void authFallback_ReturnsServiceUnavailable() throws Exception {
        mockMvc.perform(get("/fallback/auth"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("Auth service is currently unavailable. Please try again later."));
    }
}
