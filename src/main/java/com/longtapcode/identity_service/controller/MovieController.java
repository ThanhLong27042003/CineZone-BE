package com.longtapcode.identity_service.controller;

import com.longtapcode.identity_service.dto.request.ApiResponse;
import com.longtapcode.identity_service.dto.response.MovieResponse;
import com.longtapcode.identity_service.service.MovieService;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/movie")
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE ,makeFinal= true)
public class MovieController {
    MovieService movieService;
    @GetMapping("/{id}")
    public ApiResponse<MovieResponse> getMovieById(@PathVariable Long id){
        return ApiResponse.<MovieResponse>builder()
                        .result(movieService.getMovieById(id))
                .build();

    }
    @GetMapping("/getAllMovie")
    public ApiResponse<List<MovieResponse>> getAllMovie(){
        return ApiResponse.<List<MovieResponse>>builder()
                .result(movieService.getAllMovie())
                .build();
    }

    @GetMapping("getTopMovieForHomePage/{genres}")
    public ApiResponse<List<List<MovieResponse>>> getTopMovieForHomePage(@PathVariable List<String> genres){
        return ApiResponse.<List<List<MovieResponse>>>builder()
                .result(movieService.getTopMovieForHomePage(genres))
                .build();
    }
    @GetMapping("getMovieForPage/{page}/{size}")
    public ApiResponse<Page<MovieResponse>> getMovieForPage(@PathVariable("page") int page, @PathVariable("size") int size ){
        return ApiResponse.<Page<MovieResponse>>builder()
                .result(movieService.getMovieForPage(page,size))
                .build();
    }

    @GetMapping("getFavoriteMovie/{userId}")
    public ApiResponse<List<MovieResponse>> getFavoriteMovie(@PathVariable("userId") String userId){
        return ApiResponse.<List<MovieResponse>>builder()
                .result(movieService.getFavoriteMovie(userId))
                .build();
    }
    @GetMapping("isLiked/{userId}/{movieId}")
    public ApiResponse<Boolean> isLiked(@PathVariable("userId") String userId,@PathVariable("movieId") Long movieId){
        return ApiResponse.<Boolean>builder()
                .result(movieService.isLiked(userId,movieId))
                .build();
    }



}
