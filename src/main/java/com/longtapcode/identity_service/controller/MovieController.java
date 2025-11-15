package com.longtapcode.identity_service.controller;

import com.longtapcode.identity_service.dto.request.ApiResponse;
import com.longtapcode.identity_service.dto.request.MovieRequest;
import com.longtapcode.identity_service.dto.request.UpdateMovieRequest;
import com.longtapcode.identity_service.dto.response.MovieResponse;
import com.longtapcode.identity_service.service.MovieService;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
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
    @PostMapping("/createMovie")
    public ApiResponse<MovieResponse> createMovie(@RequestBody MovieRequest request){
        return ApiResponse.<MovieResponse>builder()
                .result(movieService.createMovie(request))
                .build();
    }
    @GetMapping("/getAllMovie")
    public ApiResponse<List<MovieResponse>> getAllMovie(){
        return ApiResponse.<List<MovieResponse>>builder()
                .result(movieService.getAllMovie())
                .build();
    }
    @PutMapping("updateMovie/{id}")
    public ApiResponse<String> updateMovieById(@RequestBody UpdateMovieRequest request, @PathVariable Long id){
        movieService.updateMovieById(request,id);
        return ApiResponse.<String>builder()
                .result("Update movie successful!")
                .build();
    }

    @DeleteMapping("deleteMovie/{id}")
    public ApiResponse<String> deleteMovieById(@PathVariable Long id){
        movieService.deleteMovieById(id);
        return ApiResponse.<String>builder()
                .result("Delete movie successful!")
                .build();
    }
    @GetMapping("getTopMovieForHomePage/{genres}")
    public ApiResponse<List<List<MovieResponse>>> getTopMovieForHomePage(@PathVariable List<String> genres){
        return ApiResponse.<List<List<MovieResponse>>>builder()
                .result(movieService.getTopMovieForHomePage(genres))
                .build();
    }
}
