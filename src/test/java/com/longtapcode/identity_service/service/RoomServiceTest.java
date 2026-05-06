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

import com.longtapcode.identity_service.dto.request.RoomRequest;
import com.longtapcode.identity_service.dto.response.RoomResponse;
import com.longtapcode.identity_service.entity.Room;
import com.longtapcode.identity_service.exception.AppException;
import com.longtapcode.identity_service.exception.ErrorCode;
import com.longtapcode.identity_service.mapper.RoomMapper;
import com.longtapcode.identity_service.repository.RoomRepository;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomMapper roomMapper;

    @InjectMocks
    private RoomService roomService;

    private Room room;
    private RoomResponse roomResponse;

    @BeforeEach
    void setUp() {
        room = Room.builder().roomId(1L).name("IMAX 1").build();
        roomResponse = new RoomResponse(1L, "IMAX 1");
    }

    @Nested
    @DisplayName("getAllRooms")
    class GetAllRooms {
        @Test
        @DisplayName("Success")
        void getAllRooms_Success() {
            when(roomRepository.findAll()).thenReturn(List.of(room));
            when(roomMapper.toListRoomResponse(anyList())).thenReturn(List.of(roomResponse));

            List<RoomResponse> result = roomService.getAllRooms();

            assertEquals(1, result.size());
            assertEquals("IMAX 1", result.get(0).getName());
        }
    }

    @Nested
    @DisplayName("getRoomById")
    class GetRoomById {
        @Test
        @DisplayName("Success")
        void getRoomById_Success() {
            when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
            when(roomMapper.toRoomResponse(room)).thenReturn(roomResponse);

            RoomResponse result = roomService.getRoomById(1L);

            assertEquals("IMAX 1", result.getName());
        }

        @Test
        @DisplayName("Fail - Not Found")
        void getRoomById_Fail() {
            when(roomRepository.findById(999L)).thenReturn(Optional.empty());

            AppException ex = assertThrows(AppException.class, () -> roomService.getRoomById(999L));

            assertEquals(ErrorCode.UNCATEGORIZED_EXCEPTION, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("createRoom")
    class CreateRoom {
        @Test
        @DisplayName("Success")
        void createRoom_Success() {
            RoomRequest request = new RoomRequest("IMAX 2");
            when(roomRepository.existsByName("IMAX 2")).thenReturn(false);
            when(roomMapper.toRoom(request))
                    .thenReturn(Room.builder().name("IMAX 2").build());
            when(roomMapper.toRoomResponse(any())).thenReturn(new RoomResponse(2L, "IMAX 2"));

            RoomResponse result = roomService.createRoom(request);

            assertNotNull(result);
            verify(roomRepository).save(any(Room.class));
        }

        @Test
        @DisplayName("Fail - Already Exists")
        void createRoom_Fail_Exists() {
            RoomRequest request = new RoomRequest("IMAX 1");
            when(roomRepository.existsByName("IMAX 1")).thenReturn(true);

            AppException ex = assertThrows(AppException.class, () -> roomService.createRoom(request));

            assertEquals(ErrorCode.UNCATEGORIZED_EXCEPTION, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("updateRoom")
    class UpdateRoom {
        @Test
        @DisplayName("Success")
        void updateRoom_Success() {
            RoomRequest request = new RoomRequest("IMAX Updated");
            when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
            when(roomMapper.toRoomResponse(room)).thenReturn(new RoomResponse(1L, "IMAX Updated"));

            RoomResponse result = roomService.updateRoom(1L, request);

            assertNotNull(result);
            verify(roomMapper).updateRoom(room, request);
            verify(roomRepository).save(room);
        }
    }

    @Nested
    @DisplayName("deleteRoom")
    class DeleteRoom {
        @Test
        @DisplayName("Success")
        void deleteRoom_Success() {
            when(roomRepository.existsById(1L)).thenReturn(true);

            assertDoesNotThrow(() -> roomService.deleteRoom(1L));

            verify(roomRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Fail - Not Found")
        void deleteRoom_Fail() {
            when(roomRepository.existsById(999L)).thenReturn(false);

            AppException ex = assertThrows(AppException.class, () -> roomService.deleteRoom(999L));

            assertEquals(ErrorCode.UNCATEGORIZED_EXCEPTION, ex.getErrorCode());
        }
    }
}
