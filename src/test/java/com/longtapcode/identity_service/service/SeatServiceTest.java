package com.longtapcode.identity_service.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.longtapcode.identity_service.dto.request.SeatRequest;
import com.longtapcode.identity_service.dto.response.SeatResponse;
import com.longtapcode.identity_service.entity.Seat;
import com.longtapcode.identity_service.exception.AppException;
import com.longtapcode.identity_service.exception.ErrorCode;
import com.longtapcode.identity_service.mapper.SeatMapper;
import com.longtapcode.identity_service.repository.SeatRepository;

@ExtendWith(MockitoExtension.class)
class SeatServiceTest {

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private SeatMapper seatMapper;

    @InjectMocks
    private SeatService seatService;

    private Seat seat;
    private SeatResponse seatResponse;

    @BeforeEach
    void setUp() {
        seat = Seat.builder().seatId(1).seatNumber("A1").vip(1).build();
        seatResponse = new SeatResponse(1, "A1", 1);
    }

    @Nested
    @DisplayName("getAllSeat")
    class GetAllSeat {
        @Test
        @DisplayName("Success")
        void getAllSeat_Success() {
            when(seatRepository.findAll()).thenReturn(List.of(seat));
            when(seatMapper.toListSeatResponse(anyList())).thenReturn(List.of(seatResponse));

            List<SeatResponse> result = seatService.getAllSeat();

            assertEquals(1, result.size());
            assertEquals("A1", result.get(0).getSeatNumber());
        }
    }

    @Nested
    @DisplayName("createSeat")
    class CreateSeat {
        @Test
        @DisplayName("Success")
        void createSeat_Success() {
            SeatRequest request = new SeatRequest("A1", 1);

            when(seatMapper.toSeat(request)).thenReturn(seat);
            when(seatRepository.save(any(Seat.class))).thenReturn(seat);
            when(seatMapper.toSeatResponse(seat)).thenReturn(seatResponse);

            SeatResponse result = seatService.createSeat(request);

            assertNotNull(result);
            assertEquals("A1", result.getSeatNumber());
            verify(seatRepository).save(any(Seat.class));
        }
    }

    @Nested
    @DisplayName("getSeatsByVip")
    class GetSeatsByVip {
        @Test
        @DisplayName("Success")
        void getSeatsByVip_Success() {
            when(seatRepository.findByVip(1)).thenReturn(Optional.of(List.of(seat)));
            when(seatMapper.toListSeatResponse(anyList())).thenReturn(List.of(seatResponse));

            List<SeatResponse> result = seatService.getSeatsByVip(1);

            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Fail - Not Found")
        void getSeatsByVip_Fail() {
            when(seatRepository.findByVip(99)).thenReturn(Optional.empty());

            AppException ex = assertThrows(AppException.class, () -> seatService.getSeatsByVip(99));

            assertEquals(ErrorCode.SEAT_NOT_EXISTED, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("getSeatBySeatNumber")
    class GetSeatBySeatNumber {
        @Test
        @DisplayName("Success")
        void getSeatBySeatNumber_Success() {
            when(seatRepository.findBySeatNumber("A1")).thenReturn(Optional.of(seat));
            when(seatMapper.toSeatResponse(seat)).thenReturn(seatResponse);

            SeatResponse result = seatService.getSeatBySeatNumber("A1");

            assertEquals("A1", result.getSeatNumber());
        }

        @Test
        @DisplayName("Fail - Not Found")
        void getSeatBySeatNumber_Fail() {
            when(seatRepository.findBySeatNumber("Z99")).thenReturn(Optional.empty());

            AppException ex = assertThrows(AppException.class, () -> seatService.getSeatBySeatNumber("Z99"));

            assertEquals(ErrorCode.SEAT_NOT_EXISTED, ex.getErrorCode());
        }
    }
}
