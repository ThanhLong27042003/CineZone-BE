package com.longtapcode.identity_service.service;

import com.longtapcode.identity_service.dto.request.RoomRequest;
import com.longtapcode.identity_service.dto.response.RoomResponse;
import com.longtapcode.identity_service.entity.Room;
import com.longtapcode.identity_service.exception.AppException;
import com.longtapcode.identity_service.exception.ErrorCode;
import com.longtapcode.identity_service.mapper.RoomMapper;
import com.longtapcode.identity_service.repository.RoomRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class RoomService {
    RoomRepository roomRepository;
    RoomMapper roomMapper;

    public List<RoomResponse> getAllRooms() {
        List<Room> rooms = roomRepository.findAll();
        return roomMapper.toListRoomResponse(rooms);
    }

    public RoomResponse getRoomById(Long id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION));
        return roomMapper.toRoomResponse(room);
    }

    public RoomResponse createRoom(RoomRequest request) {
        if (roomRepository.existsByName(request.getName())) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }

        Room room = roomMapper.toRoom(request);
        roomRepository.save(room);

        log.info("Created room: {}", room.getName());
        return roomMapper.toRoomResponse(room);
    }

    public RoomResponse updateRoom(Long id, RoomRequest request) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION));

        roomMapper.updateRoom(room, request);
        roomRepository.save(room);

        log.info("Updated room: {}", room.getName());
        return roomMapper.toRoomResponse(room);
    }

    public void deleteRoom(Long id) {
        if (!roomRepository.existsById(id)) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }

        roomRepository.deleteById(id);
        log.info("Deleted room with id: {}", id);
    }
}