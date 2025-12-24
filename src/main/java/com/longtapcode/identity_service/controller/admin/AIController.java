package com.longtapcode.identity_service.controller.admin;

import com.longtapcode.identity_service.dto.request.ApiResponse;
import com.longtapcode.identity_service.service.AIService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/admin/ai")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AIController {

    AIService aiService;

    /**
     * Get AI-powered analytics and insights
     */
    @GetMapping("/analytics")
    public ApiResponse<Map<String, Object>> getAIAnalytics(
            @RequestParam(required = false) LocalDateTime fromDate,
            @RequestParam(required = false) LocalDateTime toDate
    ) {
        try {
            // Get bookings data
            List<Map<String, Object>> bookings = aiService.prepareBookingsData(fromDate, toDate);

            // Call AI service for analysis
            Map<String, Object> analysis = aiService.analyzeRevenue(
                    bookings,
                    fromDate != null ? fromDate : LocalDateTime.of(2024, 1, 1, 0, 0),
                    toDate != null ? toDate : LocalDateTime.of(2025, 12, 31, 23, 59, 59)
            );

            return ApiResponse.<Map<String, Object>>builder()
                    .result(analysis)
                    .message("AI analytics generated successfully")
                    .build();

        } catch (Exception e) {
            log.error("Failed to get AI analytics: {}", e.getMessage());
            return ApiResponse.<Map<String, Object>>builder()
                    .code(500)
                    .message("Failed to generate AI analytics")
                    .result(Collections.emptyMap())
                    .build();
        }
    }

    /**
     * Generate AI-optimized schedule for movies
     */
    @PostMapping("/optimize-schedule")
    public ApiResponse<List<Map<String, Object>>> optimizeSchedule(
            @RequestBody Map<String, Object> request
    ) {
        try {
            // Extract request parameters
            List<Integer> roomIds = (List<Integer>) request.get("rooms");
            Map<String, String> dateRange = (Map<String, String>) request.get("dateRange");
            Map<String, Object> constraints = (Map<String, Object>) request.getOrDefault("constraints", new HashMap<>());

            // Get movies and existing shows
            List<Map<String, Object>> movies = aiService.prepareMoviesData();
            List<Map<String, Object>> existingShows = aiService.prepareShowsData();

            // Call AI service for optimization
            List<Map<String, Object>> optimizedSchedule = aiService.optimizeSchedule(
                    movies,
                    existingShows,
                    roomIds,
                    dateRange,
                    constraints
            );

            return ApiResponse.<List<Map<String, Object>>>builder()
                    .result(optimizedSchedule)
                    .message(String.format("Generated %d optimized shows", optimizedSchedule.size()))
                    .build();

        } catch (Exception e) {
            log.error("Failed to optimize schedule: {}", e.getMessage());
            return ApiResponse.<List<Map<String, Object>>>builder()
                    .code(500)
                    .message("Failed to generate optimized schedule")
                    .result(Collections.emptyList())
                    .build();
        }
    }

    /**
     * Train AI model with historical data
     */
    @PostMapping("/train-model")
    public ApiResponse<Map<String, Object>> trainModel() {
        try {
            // Get all historical bookings
            List<Map<String, Object>> historicalData = aiService.prepareBookingsData(null, null);

            // Train model
            boolean success = aiService.trainModel(historicalData);

            Map<String, Object> result = new HashMap<>();
            result.put("success", success);
            result.put("dataPoints", historicalData.size());
            result.put("message", success ? "Model training started" : "Training failed");

            return ApiResponse.<Map<String, Object>>builder()
                    .result(result)
                    .build();

        } catch (Exception e) {
            log.error("Failed to train model: {}", e.getMessage());
            return ApiResponse.<Map<String, Object>>builder()
                    .code(500)
                    .message("Failed to train AI model")
                    .build();
        }
    }

    /**
     * Predict demand for specific time slot
     */
    @GetMapping("/predict-demand")
    public ApiResponse<Map<String, Object>> predictDemand(
            @RequestParam int hour,
            @RequestParam int dayOfWeek,
            @RequestParam boolean isWeekend
    ) {
        Map<String, Object> prediction = aiService.predictDemand(hour, dayOfWeek, isWeekend);

        return ApiResponse.<Map<String, Object>>builder()
                .result(prediction)
                .build();
    }

    /**
     * Check AI service status
     */
    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> getAIServiceStatus() {
        boolean available = aiService.isAIServiceAvailable();

        Map<String, Object> status = new HashMap<>();
        status.put("available", available);
        status.put("service", "CineZone AI Service");
        status.put("features", Arrays.asList(
                "Revenue Analytics",
                "Schedule Optimization",
                "Demand Prediction",
                "Pattern Recognition"
        ));

        return ApiResponse.<Map<String, Object>>builder()
                .result(status)
                .message(available ? "AI service operational" : "AI service unavailable")
                .build();
    }
}