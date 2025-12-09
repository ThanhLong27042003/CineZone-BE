package com.longtapcode.identity_service.controller.admin;

import com.longtapcode.identity_service.dto.request.ApiResponse;
import com.longtapcode.identity_service.dto.request.admin.MovieRequest;
import com.longtapcode.identity_service.dto.request.admin.UpdateMovieRequest;
import com.longtapcode.identity_service.dto.response.MovieResponse;
import com.longtapcode.identity_service.service.MovieService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/movies")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
//@PreAuthorize("hasRole('ADMIN')")
public class AdminMovieController {
    MovieService movieService;

    @GetMapping("/getAllMovies/{page}/{size}")
    public ApiResponse<Page<MovieResponse>> getAllMoviesForAdmin(
            @PathVariable("page") int page,
            @PathVariable("size") int size,
            @RequestParam(required = false) String search) {
        return ApiResponse.<Page<MovieResponse>>builder()
                .result(movieService.getAllMoviesForAdmin(page,size,search))
                .build();
    }

    @PostMapping
    public ApiResponse<MovieResponse> createMovie(@RequestBody MovieRequest request) {
        return ApiResponse.<MovieResponse>builder()
                .result(movieService.createMovie(request))
                .build();
    }

    @PutMapping("/{movieId}")
    public ApiResponse<MovieResponse> updateMovie(
            @PathVariable Long movieId,
            @RequestBody UpdateMovieRequest request) {
        return ApiResponse.<MovieResponse>builder()
                .result(movieService.updateMovie(movieId, request))
                .build();
    }

    @DeleteMapping("/{movieId}")
    public ApiResponse<Void> deleteMovie(@PathVariable Long movieId) {
        movieService.deleteMovieById(movieId);
        return ApiResponse.<Void>builder()
                .message("Movie deleted successfully")
                .build();
    }

}
