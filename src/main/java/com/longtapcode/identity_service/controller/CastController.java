//package com.longtapcode.identity_service.controller;
//
//import com.longtapcode.identity_service.dto.request.ApiResponse;
//import com.longtapcode.identity_service.dto.response.CastResponse;
//import com.longtapcode.identity_service.service.CastService;
//import lombok.AccessLevel;
//import lombok.AllArgsConstructor;
//import lombok.experimental.FieldDefaults;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PathVariable;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//import java.util.List;
//
//@RestController
//@RequestMapping("/cast")
//@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
//@AllArgsConstructor
//public class CastController {
//    CastService castService;
//    @GetMapping("/getAllCast")
//    public ApiResponse<List<CastResponse>> getAllCast(){
//        return ApiResponse.<List<CastResponse>>builder()
//                .result(castService.getAllCast())
//                .build();
//    }
//
//    @GetMapping("/{castId}")
//    public ApiResponse<CastResponse> getCastById(@PathVariable Long castId){
//        return ApiResponse.<CastResponse>builder()
//                .result(castService.getCastById(castId))
//                .build();
//    }
//}

package com.longtapcode.identity_service.controller;

import com.longtapcode.identity_service.dto.request.ApiResponse;
import com.longtapcode.identity_service.dto.request.CastRequest;
import com.longtapcode.identity_service.dto.response.CastResponse;
import com.longtapcode.identity_service.service.CastService;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/{castId}")
    public ApiResponse<CastResponse> getCastById(@PathVariable Long castId){
        return ApiResponse.<CastResponse>builder()
                .result(castService.getCastById(castId))
                .build();
    }


    @PostMapping()
    public ApiResponse<CastResponse> createCast(@RequestBody CastRequest request) {
        return ApiResponse.<CastResponse>builder()
                .result(castService.createCast(request))
                .build();
    }


    @PutMapping("/{castId}")
    public ApiResponse<CastResponse> updateCast(@PathVariable Long castId, @org.springframework.web.bind.annotation.RequestBody com.longtapcode.identity_service.dto.request.CastRequest request) {
        return ApiResponse.<CastResponse>builder()
                .result(castService.updateCast(castId, request))
                .build();
    }


    @DeleteMapping("/{castId}")
    public ApiResponse<String> deleteCast(@PathVariable Long castId) {
        castService.deleteCast(castId);
        return ApiResponse.<String>builder()
                .result("Cast deleted successfully")
                .build();
    }
}

