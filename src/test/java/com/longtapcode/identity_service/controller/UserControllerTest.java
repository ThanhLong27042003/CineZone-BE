package com.longtapcode.identity_service.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.longtapcode.identity_service.dto.request.UpdateUserRequest;
import com.longtapcode.identity_service.service.UserService;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable Spring Security filters for unit testing controller
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    private ObjectMapper objectMapper;

    private UpdateUserRequest updateRequest;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule()); // Support LocalDate serialization

        updateRequest = new UpdateUserRequest(
                "updatedUser",
                "John",
                "Doe",
                "0123456789",
                "123 Street",
                "john@gmail.com",
                "avatar.png",
                LocalDate.of(1990, 1, 1));
    }

    @Test
    @DisplayName("Update User - Success")
    void updateUser_Success() throws Exception {
        // Arrange
        doNothing().when(userService).updateUserService(anyString(), any());

        // Act & Assert
        mockMvc.perform(put("/user/updateUser/testId")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("Update user successful!"));
    }

    @Test
    @DisplayName("Add Favorite Movie - Success")
    void addFavoriteMovie_Success() throws Exception {
        // Arrange
        doNothing().when(userService).addFavoriteMovie(anyString(), any(Long.class));

        // Act & Assert
        mockMvc.perform(put("/user/addFavoriteMovie/testId/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("Add your favorite movie list successful!"));
    }
}
