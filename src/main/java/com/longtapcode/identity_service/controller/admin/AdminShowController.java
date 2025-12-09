package com.longtapcode.identity_service.controller.admin;

import com.longtapcode.identity_service.dto.request.ApiResponse;
import com.longtapcode.identity_service.dto.request.ShowRequest;
import com.longtapcode.identity_service.dto.request.UpdateShowRequest;
import com.longtapcode.identity_service.dto.response.ShowResponse;
import com.longtapcode.identity_service.service.ShowService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/admin/shows")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
//@PreAuthorize("hasRole('ADMIN')")
public class AdminShowController {
    ShowService showService;

    @GetMapping("/getAllShows/{page}/{size}")
    public ApiResponse<Page<ShowResponse>> getAllShowsForAdmin(
            @PathVariable("page") int page,
            @PathVariable("size") int size,
            @RequestParam(required = false) Long movieId,
            @RequestParam(required = false) LocalDateTime dateTime) {
        return ApiResponse.<Page<ShowResponse>>builder()
                .result(showService.getAllShowsForAdmin(page,size, movieId, dateTime))
                .build();
    }

    @PostMapping
    public ApiResponse<ShowResponse> createShow(@RequestBody ShowRequest request) {
        return ApiResponse.<ShowResponse>builder()
                .result(showService.createShow(request))
                .build();
    }

    @PutMapping("/{showId}")
    public ApiResponse<ShowResponse> updateShow(
            @PathVariable Long showId,
            @RequestBody UpdateShowRequest request) {
        return ApiResponse.<ShowResponse>builder()
                .result(showService.updateShow(showId, request))
                .build();
    }

    @DeleteMapping("/{showId}")
    public ApiResponse<Void> deleteShow(@PathVariable Long showId) {
        showService.deleteShow(showId);
        return ApiResponse.<Void>builder()
                .message("Show deleted successfully")
                .build();
    }
}
