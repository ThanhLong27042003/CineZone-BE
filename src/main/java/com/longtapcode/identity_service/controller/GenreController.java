package com.longtapcode.identity_service.controller;

import com.longtapcode.identity_service.dto.request.ApiResponse;
import com.longtapcode.identity_service.dto.response.GenreResponse;
import com.longtapcode.identity_service.service.GenreService;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/genre")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class GenreController {
    GenreService genreService;
    @GetMapping("/getAllGenre")
    public ApiResponse<List<GenreResponse>> getAllGenre(){
        return ApiResponse.<List<GenreResponse>>builder()
                .result(genreService.getAllGenre())
                .build();
    }

    @GetMapping("/{genreId}")
    public ApiResponse<GenreResponse> getGenreById(@PathVariable Long genreId){
        return ApiResponse.<GenreResponse>builder()
                .result(genreService.getGenreById(genreId))
                .build();
    }
}
