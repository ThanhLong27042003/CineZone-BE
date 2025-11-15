package com.longtapcode.identity_service.controller;

import com.longtapcode.identity_service.dto.request.ApiResponse;
import com.longtapcode.identity_service.dto.response.CastResponse;
import com.longtapcode.identity_service.service.CastService;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/cast")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class CastController {
    CastService castService;
    @GetMapping("/getAllCast")
    public ApiResponse<List<CastResponse>> getAllCast(){
        return ApiResponse.<List<CastResponse>>builder()
                .result(castService.getAllCast())
                .build();
    }
}
