package com.longtapcode.identity_service.controller;


import com.longtapcode.identity_service.dto.request.ApiResponse;
import com.longtapcode.identity_service.dto.response.CastResponse;
import com.longtapcode.identity_service.dto.response.MovieResponse;
import com.longtapcode.identity_service.service.CastService;
import com.longtapcode.identity_service.service.MovieService;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/general")
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE ,makeFinal= true)
public class GeneralController {
    MovieService movieService;
    CastService castService;
    @GetMapping("search/{keyword}")
    public ApiResponse<?> search(@PathVariable String keyword){
        List<MovieResponse> movieResponse = movieService.searchMovies(keyword);
        List<CastResponse> castResponse = castService.searchCasts(keyword);
        if(!movieResponse.isEmpty()){
            return ApiResponse.<List<MovieResponse>>builder()
                    .result(movieResponse)
                    .build();
        } else if (!castResponse.isEmpty()) {
            return ApiResponse.<List<CastResponse>>builder()
                    .result(castResponse)
                    .build();
        }else{
            return ApiResponse.<String>builder()
                    .result("Không tìm thấy kết quả nào!!!")
                    .build();
        }
    }
}
