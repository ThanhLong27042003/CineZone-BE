package com.longtapcode.identity_service.controller;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.longtapcode.identity_service.dto.response.MovieResponse;
import com.longtapcode.identity_service.service.MovieService;

@WebMvcTest(MovieController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security filters for simple unit test of controller
public class MovieControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MovieService movieService;

    private MovieResponse movieResponse;

    @BeforeEach
    void setUp() {
        movieResponse = MovieResponse.builder().id(1L).title("Test Movie").build();
    }

    @Test
    void getMovieById_Success() throws Exception {
        when(movieService.getMovieById(anyLong())).thenReturn(movieResponse);

        mockMvc.perform(get("/movie/1").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.id").value(1))
                .andExpect(jsonPath("$.result.title").value("Test Movie"));
    }

    @Test
    void getAllMovie_Success() throws Exception {
        when(movieService.getAllMovie()).thenReturn(List.of(movieResponse));

        mockMvc.perform(get("/movie/getAllMovie").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result[0].id").value(1))
                .andExpect(jsonPath("$.result[0].title").value("Test Movie"));
    }
}
