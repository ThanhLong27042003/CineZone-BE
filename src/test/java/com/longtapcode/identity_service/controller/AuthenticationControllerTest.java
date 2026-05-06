package com.longtapcode.identity_service.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.longtapcode.identity_service.dto.request.AuthenticationRequest;
import com.longtapcode.identity_service.dto.response.AuthenticationResponse;
import com.longtapcode.identity_service.service.AuthenticationService;

@WebMvcTest(AuthenticationController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security filters for simple unit test of controller
public class AuthenticationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthenticationService authenticationService;

    private AuthenticationRequest request;
    private AuthenticationResponse response;

    @BeforeEach
    void setUp() {
        request = new AuthenticationRequest("testUser", "password123");
        response = AuthenticationResponse.builder()
                .token("sample-jwt-token")
                .authenticated(true)
                .build();
    }

    @Test
    void logIn_Success() throws Exception {
        when(authenticationService.logIn(any(), any())).thenReturn(response);

        mockMvc.perform(post("/auth/log-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.token").value("sample-jwt-token"))
                .andExpect(jsonPath("$.result.authenticated").value(true));
    }
}
