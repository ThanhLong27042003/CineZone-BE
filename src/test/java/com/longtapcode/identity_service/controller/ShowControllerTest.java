package com.longtapcode.identity_service.controller;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.longtapcode.identity_service.dto.request.ShowRequest;
import com.longtapcode.identity_service.dto.response.ShowResponse;
import com.longtapcode.identity_service.service.ShowService;

@WebMvcTest(ShowController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security filters for unit test
public class ShowControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ShowService showService;

    private ShowResponse showResponse;
    private ShowRequest showRequest;

    @BeforeEach
    void setUp() {
        showResponse = new ShowResponse();
        showResponse.setShowId(1L);
        showResponse.setMovieId(1L);
        showResponse.setRoomId(1L);
        showResponse.setRoomName("IMAX 1");
        showResponse.setShowDate(LocalDate.now().plusDays(1));
        showResponse.setShowTime(LocalTime.of(19, 0));
        showResponse.setPrice(new BigDecimal("100000"));

        showRequest = new ShowRequest();
        showRequest.setMovieId(1L);
        showRequest.setRoomId(1L);
        showRequest.setShowDate(LocalDate.now().plusDays(1));
        showRequest.setShowTime(LocalTime.of(19, 0));
        showRequest.setPrice(new BigDecimal("100000"));
    }

    @Test
    void getShowById_Success() throws Exception {
        when(showService.getShowById(anyLong())).thenReturn(showResponse);

        mockMvc.perform(get("/show/1").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.showId").value(1))
                .andExpect(jsonPath("$.result.roomName").value("IMAX 1"));
    }

    @Test
    void getAllShow_Success() throws Exception {
        when(showService.getAllShow()).thenReturn(List.of(showResponse));

        mockMvc.perform(get("/show/getAllShow").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result[0].showId").value(1))
                .andExpect(jsonPath("$.result[0].price").value(100000));
    }
}
