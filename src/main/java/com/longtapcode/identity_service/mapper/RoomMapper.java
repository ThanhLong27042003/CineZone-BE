package com.longtapcode.identity_service.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.longtapcode.identity_service.dto.request.RoomRequest;
import com.longtapcode.identity_service.dto.response.RoomResponse;
import com.longtapcode.identity_service.entity.Room;

@Mapper(componentModel = "spring")
public interface RoomMapper {
    Room toRoom(RoomRequest request);

    @Mapping(target = "id", source = "roomId")
    RoomResponse toRoomResponse(Room room);

    List<RoomResponse> toListRoomResponse(List<Room> rooms);

    void updateRoom(@MappingTarget Room room, RoomRequest request);
}
