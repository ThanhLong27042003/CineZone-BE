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
    @PostMapping("/createShow")
    public ApiResponse<ShowResponse> createShow(@RequestBody ShowRequest request){
        return ApiResponse.<ShowResponse>builder()
                .result(showService.createShow(request))
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
    @PutMapping("updateShow/{id}")
    public ApiResponse<String> updateShowById(@RequestBody UpdateShowRequest request, @PathVariable Long id){
        showService.updateShowById(request,id);
        return ApiResponse.<String>builder()
                .result("Update show successful!")
                .build();
    }

    @DeleteMapping("deleteShow/{id}")
    public ApiResponse<String> deleteShowById(@PathVariable Long id){
        showService.deleteShowById(id);
        return ApiResponse.<String>builder()
                .result("Delete show successful!")
                .build();
    }

}
