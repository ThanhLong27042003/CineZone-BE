//package com.longtapcode.identity_service.controller;
//
//import com.longtapcode.identity_service.dto.request.ApiResponse;
//import com.longtapcode.identity_service.dto.request.GenreRequest;
//import com.longtapcode.identity_service.dto.response.GenreResponse;
//import com.longtapcode.identity_service.service.GenreService;
//import lombok.AccessLevel;
//import lombok.AllArgsConstructor;
//import lombok.experimental.FieldDefaults;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//
//@RestController
//@RequestMapping("/genre")
//@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
//@AllArgsConstructor
//public class GenreController {
//    GenreService genreService;
//    @GetMapping("/getAllGenre")
//    public ApiResponse<List<GenreResponse>> getAllGenre(){
//        return ApiResponse.<List<GenreResponse>>builder()
//                .result(genreService.getAllGenre())
//                .build();
//    }
//
//    @GetMapping("/{genreId}")
//    public ApiResponse<GenreResponse> getGenreById(@PathVariable Long genreId){
//        return ApiResponse.<GenreResponse>builder()
//                .result(genreService.getGenreById(genreId))
//                .build();
//    }
//
//
//    @PostMapping
//    public ApiResponse<GenreResponse> createGenre(@RequestBody GenreRequest request) {
//        return ApiResponse.<GenreResponse>builder()
//                .result(genreService.createGenre(request))
//                .build();
//    }
//
//    @PutMapping("/{genreId}")
//    public ApiResponse<GenreResponse> updateGenre(@PathVariable Long genreId, @RequestBody GenreRequest request) {
//        return ApiResponse.<GenreResponse>builder()
//                .result(genreService.updateGenre(genreId, request))
//                .build();
//    }
//
//    @DeleteMapping("/{genreId}")
//    public ApiResponse<String> deleteGenre(@PathVariable Long genreId) {
//        genreService.deleteGenre(genreId);
//        return ApiResponse.<String>builder()
//                .result("Genre deleted successfully")
//                .build();
//    }
//}
//

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
    public ApiResponse<Object> getAllGenre(
            @org.springframework.web.bind.annotation.RequestParam(required = false, defaultValue = "-1") int page,
            @org.springframework.web.bind.annotation.RequestParam(required = false, defaultValue = "10") int size,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String search
    ){
        if (page == -1) {
            return ApiResponse.builder().result(genreService.getAllGenre()).build();
        }
        return ApiResponse.builder()
                .result(genreService.getAllGenres(page, size, search))
                .build();
    }

    @GetMapping("/{genreId}")
    public ApiResponse<GenreResponse> getGenreById(@PathVariable Long genreId){
        return ApiResponse.<GenreResponse>builder()
                .result(genreService.getGenreById(genreId))
                .build();
    }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    @org.springframework.web.bind.annotation.PostMapping
    public ApiResponse<GenreResponse> createGenre(@org.springframework.web.bind.annotation.RequestBody com.longtapcode.identity_service.dto.request.GenreRequest request) {
        return ApiResponse.<GenreResponse>builder()
                .result(genreService.createGenre(request))
                .build();
    }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    @org.springframework.web.bind.annotation.PutMapping("/{genreId}")
    public ApiResponse<GenreResponse> updateGenre(@PathVariable Long genreId, @org.springframework.web.bind.annotation.RequestBody com.longtapcode.identity_service.dto.request.GenreRequest request) {
        return ApiResponse.<GenreResponse>builder()
                .result(genreService.updateGenre(genreId, request))
                .build();
    }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    @org.springframework.web.bind.annotation.DeleteMapping("/{genreId}")
    public ApiResponse<String> deleteGenre(@PathVariable Long genreId) {
        genreService.deleteGenre(genreId);
        return ApiResponse.<String>builder()
                .result("Genre deleted successfully")
                .build();
    }
}

