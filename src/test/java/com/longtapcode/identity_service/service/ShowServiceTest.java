package com.longtapcode.identity_service.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.longtapcode.identity_service.dto.request.ShowRequest;
import com.longtapcode.identity_service.dto.request.UpdateShowRequest;
import com.longtapcode.identity_service.dto.response.ShowResponse;
import com.longtapcode.identity_service.entity.Movie;
import com.longtapcode.identity_service.entity.Room;
import com.longtapcode.identity_service.entity.Show;
import com.longtapcode.identity_service.exception.AppException;
import com.longtapcode.identity_service.exception.ErrorCode;
import com.longtapcode.identity_service.mapper.ShowMapper;
import com.longtapcode.identity_service.repository.BookingRepository;
import com.longtapcode.identity_service.repository.MovieRepository;
import com.longtapcode.identity_service.repository.RoomRepository;
import com.longtapcode.identity_service.repository.ShowRepository;

@ExtendWith(MockitoExtension.class)
class ShowServiceTest {

    @Mock
    private ShowRepository showRepository;

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private ShowMapper showMapper;

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private ShowService showService;

    private Show show;
    private Movie movie;
    private Room room;
    private ShowResponse showResponse;
    private LocalDateTime futureDateTime;
    private LocalDateTime pastDateTime;

    @BeforeEach
    void setUp() {
        futureDateTime = LocalDateTime.now().plusDays(1);
        pastDateTime = LocalDateTime.now().minusDays(1);

        movie = Movie.builder().id(1L).title("Test Movie").runtime(120).build();
        room = Room.builder().roomId(1L).name("Room 1").build();

        show = Show.builder()
                .id(1L)
                .movieID(movie)
                .roomId(room)
                .showDateTime(futureDateTime)
                .price(new BigDecimal("100000"))
                .build();

        showResponse = new ShowResponse();
        showResponse.setShowId(1L);
        showResponse.setShowDate(futureDateTime.toLocalDate());
        showResponse.setShowTime(futureDateTime.toLocalTime());
    }

    @Nested
    @DisplayName("getShowById")
    class GetShowById {

        @Test
        @DisplayName("Success - Future show")
        void getShowById_Success() {
            when(showRepository.findById(1L)).thenReturn(Optional.of(show));
            when(showMapper.toShowResponse(show)).thenReturn(showResponse);

            ShowResponse result = showService.getShowById(1L);

            assertNotNull(result);
            assertEquals(1L, result.getShowId());
        }

        @Test
        @DisplayName("Fail - Show already passed")
        void getShowById_Fail_AlreadyPassed() {
            show.setShowDateTime(pastDateTime);
            when(showRepository.findById(1L)).thenReturn(Optional.of(show));

            AppException ex = assertThrows(AppException.class, () -> showService.getShowById(1L));

            assertEquals(ErrorCode.SHOW_ALREADY_PASSED, ex.getErrorCode());
        }

        @Test
        @DisplayName("Fail - Not found")
        void getShowById_Fail_NotFound() {
            when(showRepository.findById(999L)).thenReturn(Optional.empty());

            AppException ex = assertThrows(AppException.class, () -> showService.getShowById(999L));

            assertEquals(ErrorCode.SHOW_NOT_EXISTED, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("getAllShow (User)")
    class GetAllShow {

        @Test
        @DisplayName("Success - Returns only upcoming shows")
        void getAllShow_ReturnsUpcomingOnly() {
            Show pastShow = Show.builder().id(2L).showDateTime(pastDateTime).build();
            when(showRepository.findAll()).thenReturn(List.of(show, pastShow));
            when(showMapper.toListShowResponse(anyList())).thenReturn(List.of(showResponse));

            List<ShowResponse> result = showService.getAllShow();

            assertEquals(1, result.size());
            verify(showMapper)
                    .toListShowResponse(argThat(
                            list -> list.size() == 1 && list.get(0).getId().equals(1L)));
        }
    }

    @Nested
    @DisplayName("createShow (Admin)")
    class CreateShow {

        @Test
        @DisplayName("Success")
        void createShow_Success() {
            ShowRequest request = new ShowRequest();
            request.setMovieId(1L);
            request.setRoomId(1L);
            request.setShowDate(futureDateTime.toLocalDate());
            request.setShowTime(futureDateTime.toLocalTime());

            when(movieRepository.findById(1L)).thenReturn(Optional.of(movie));
            when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
            when(showRepository.countOverlappingShow(anyLong(), any(), any(), any()))
                    .thenReturn(0L);
            when(showMapper.toShow(any())).thenReturn(show);

            assertDoesNotThrow(() -> showService.createShow(request));

            verify(showRepository).save(show);
        }

        @Test
        @DisplayName("Fail - Invalid show time (Past)")
        void createShow_Fail_PastTime() {
            ShowRequest request = new ShowRequest();
            request.setMovieId(1L);
            request.setRoomId(1L);
            request.setShowDate(pastDateTime.toLocalDate());
            request.setShowTime(pastDateTime.toLocalTime());

            when(movieRepository.findById(1L)).thenReturn(Optional.of(movie));
            when(roomRepository.findById(1L)).thenReturn(Optional.of(room));

            AppException ex = assertThrows(AppException.class, () -> showService.createShow(request));

            assertEquals(ErrorCode.INVALID_SHOW_TIME, ex.getErrorCode());
        }

        @Test
        @DisplayName("Fail - Show overlap")
        void createShow_Fail_Overlap() {
            ShowRequest request = new ShowRequest();
            request.setMovieId(1L);
            request.setRoomId(1L);
            request.setShowDate(futureDateTime.toLocalDate());
            request.setShowTime(futureDateTime.toLocalTime());

            when(movieRepository.findById(1L)).thenReturn(Optional.of(movie));
            when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
            when(showRepository.countOverlappingShow(anyLong(), any(), any(), any()))
                    .thenReturn(1L);

            AppException ex = assertThrows(AppException.class, () -> showService.createShow(request));

            assertEquals(ErrorCode.SHOW_OVERLAP, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("updateShow (Admin)")
    class UpdateShow {

        @Test
        @DisplayName("Success")
        void updateShow_Success() {
            UpdateShowRequest request = new UpdateShowRequest();
            request.setMovieId(1L);
            request.setRoomId(1L);
            request.setShowDate(futureDateTime.toLocalDate());
            request.setShowTime(futureDateTime.toLocalTime());

            when(showRepository.findById(1L)).thenReturn(Optional.of(show));
            when(bookingRepository.existsByShowID(show)).thenReturn(false);
            when(movieRepository.findById(1L)).thenReturn(Optional.of(movie));
            when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
            when(showRepository.countOverlappingShow(anyLong(), any(), any(), any()))
                    .thenReturn(0L);

            assertDoesNotThrow(() -> showService.updateShow(1L, request));

            verify(showRepository).save(show);
        }

        @Test
        @DisplayName("Fail - Show has bookings")
        void updateShow_Fail_HasBookings() {
            UpdateShowRequest request = new UpdateShowRequest();

            when(showRepository.findById(1L)).thenReturn(Optional.of(show));
            when(bookingRepository.existsByShowID(show)).thenReturn(true);

            AppException ex = assertThrows(AppException.class, () -> showService.updateShow(1L, request));

            assertEquals(ErrorCode.SHOW_HAS_BOOKINGS, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("deleteShow (Admin)")
    class DeleteShow {

        @Test
        @DisplayName("Success")
        void deleteShow_Success() {
            when(showRepository.findById(1L)).thenReturn(Optional.of(show));
            when(bookingRepository.existsByShowID(show)).thenReturn(false);

            assertDoesNotThrow(() -> showService.deleteShow(1L));

            verify(showRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Fail - Show has bookings")
        void deleteShow_Fail_HasBookings() {
            when(showRepository.findById(1L)).thenReturn(Optional.of(show));
            when(bookingRepository.existsByShowID(show)).thenReturn(true);

            AppException ex = assertThrows(AppException.class, () -> showService.deleteShow(1L));

            assertEquals(ErrorCode.SHOW_HAS_BOOKINGS, ex.getErrorCode());
        }
    }
}
