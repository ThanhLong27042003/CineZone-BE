package com.longtapcode.identity_service.mapper;

import com.longtapcode.identity_service.dto.request.SeatRequest;
import com.longtapcode.identity_service.dto.response.SeatResponse;
import com.longtapcode.identity_service.entity.Seat;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SeatMapper {
    List<SeatResponse> toListSeatResponse(List<Seat> seats);
    Seat toSeat(SeatRequest seatRequest);
    SeatResponse toSeatResponse(Seat seat);
}
