package com.longtapcode.identity_service.mapper;

import java.util.List;

import org.mapstruct.Mapper;

import com.longtapcode.identity_service.dto.request.SeatRequest;
import com.longtapcode.identity_service.dto.response.SeatResponse;
import com.longtapcode.identity_service.entity.Seat;

@Mapper(componentModel = "spring")
public interface SeatMapper {
    List<SeatResponse> toListSeatResponse(List<Seat> seats);

    Seat toSeat(SeatRequest seatRequest);

    SeatResponse toSeatResponse(Seat seat);
}
