package com.longtapcode.identity_service.controller;

import com.longtapcode.identity_service.dto.request.ApiResponse;
import com.longtapcode.identity_service.dto.request.SeatHoldRequest;
import com.longtapcode.identity_service.dto.request.SeatRequest;
import com.longtapcode.identity_service.dto.response.SeatResponse;
import com.longtapcode.identity_service.dto.response.SeatUpdateResponse;
import com.longtapcode.identity_service.dto.response.SeatUpdateSuccess;
import com.longtapcode.identity_service.service.SeatHoldService;
import com.longtapcode.identity_service.service.SeatService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/seat")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SeatController {
    SeatService seatService;
    SeatHoldService seatHoldService;
    @GetMapping("/getAllSeat")
    public ApiResponse<List<SeatResponse>> getAllSeat(){
        return ApiResponse.<List<SeatResponse>>builder()
                .result(seatService.getAllSeat())
                .build();

    }

    @PostMapping("/createSeat")
    public ApiResponse<SeatResponse> createSeat(@RequestBody SeatRequest seatRequest){
        return ApiResponse.<SeatResponse>builder()
                .result(seatService.createSeat(seatRequest))
                .build();
    }
    @GetMapping("/getSeatsByVip/{vip}")
    public ApiResponse<List<SeatResponse>> getSeatsByVip(@PathVariable("vip") int vip){
        return ApiResponse.<List<SeatResponse>>builder()
                .result(seatService.getSeatsByVip(vip))
                .build();
    }


    @PostMapping("/hold")
    public ApiResponse<SeatUpdateSuccess> holdSeat(@RequestBody SeatHoldRequest request) {
        return ApiResponse.<SeatUpdateSuccess>builder()
                        .result(seatHoldService.holdSeat(request))
                .build();
    }

    // ==================== RELEASE GHẾ ====================
    @PostMapping("/release")
    public ApiResponse<SeatUpdateSuccess> releaseSeat(@RequestBody SeatHoldRequest request) {
        return ApiResponse.<SeatUpdateSuccess>builder()
                .result(seatHoldService.releaseSeat(request))
                .build();
    }

    @GetMapping("/occupied/{showId}")
    public ApiResponse<List<SeatUpdateResponse>> getOccupiedSeats(@PathVariable("showId") Long showId) {
        return ApiResponse.<List<SeatUpdateResponse>>builder()
                .result(seatHoldService.getOccupiedSeats(showId))
                .build();
    }

}
