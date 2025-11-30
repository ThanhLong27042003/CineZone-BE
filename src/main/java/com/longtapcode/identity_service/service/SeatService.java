package com.longtapcode.identity_service.service;

import com.longtapcode.identity_service.dto.request.SeatRequest;
import com.longtapcode.identity_service.dto.response.SeatResponse;
import com.longtapcode.identity_service.entity.Seat;
import com.longtapcode.identity_service.entity.SeatInstance;
import com.longtapcode.identity_service.entity.Show;
import com.longtapcode.identity_service.exception.AppException;
import com.longtapcode.identity_service.exception.ErrorCode;
import com.longtapcode.identity_service.mapper.SeatMapper;
import com.longtapcode.identity_service.repository.SeatRepository;
import com.longtapcode.identity_service.repository.ShowRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SeatService {
    SeatMapper seatMapper;
    SeatRepository seatRepository;
    private final com.longtapcode.identity_service.repository.seatInstanceRepository seatInstanceRepository;

    public List<SeatResponse> getAllSeat(){
        List<Seat> seats = seatRepository.findAll();
        return seatMapper.toListSeatResponse(seats);
    }

    public SeatResponse createSeat(SeatRequest seatRequest){
        Seat seat = seatMapper.toSeat(seatRequest);
        seatRepository.save(seat);
        return seatMapper.toSeatResponse(seat);
    }

    public List<SeatResponse> getSeatsByVip(int vip){
        List<Seat> seats = seatRepository.findByVip(vip).orElseThrow(()-> new AppException(ErrorCode.SEAT_NOT_EXISTED));
        return seatMapper.toListSeatResponse(seats);
    }
}
