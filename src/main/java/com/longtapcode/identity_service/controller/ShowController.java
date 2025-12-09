package com.longtapcode.identity_service.controller;

import com.longtapcode.identity_service.dto.request.ApiResponse;
import com.longtapcode.identity_service.dto.request.ShowRequest;
import com.longtapcode.identity_service.dto.request.UpdateShowRequest;
import com.longtapcode.identity_service.dto.response.ShowResponse;
import com.longtapcode.identity_service.service.ShowService;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/show")
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE ,makeFinal= true)
public class ShowController {
    ShowService showService;
    @GetMapping("/{id}")
    public ApiResponse<ShowResponse> getShowById(@PathVariable Long id){
        return ApiResponse.<ShowResponse>builder()
                .result(showService.getShowById(id))
                .build();

    }
    @GetMapping("/getAllShow")
    public ApiResponse<List<ShowResponse>> getAllShow(){
        return ApiResponse.<List<ShowResponse>>builder()
                .result(showService.getAllShow())
                .build();
    }
    @GetMapping("getAllShow/{movieId}")
    public ApiResponse<List<ShowResponse>> getAllShowByMovieId(@PathVariable Long movieId){
        return ApiResponse.<List<ShowResponse>>builder()
                .result(showService.getAllShowByMovieId(movieId))
                .build();
    }

}
