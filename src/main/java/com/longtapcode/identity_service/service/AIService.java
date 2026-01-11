package com.longtapcode.identity_service.service;

import com.longtapcode.identity_service.entity.Movie;
import com.longtapcode.identity_service.entity.Show;
import com.longtapcode.identity_service.repository.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
public class AIService {

    final RestTemplate restTemplate = new RestTemplate();
    private final BookingRepository bookingRepository;
    private final GenreRepository genreRepository;
    private final CastRepository castRepository;
    private final MovieRepository movieRepository;
    private final ShowRepository showRepository;

    @Value("${ai.service.url:http://localhost:8000}")
    String aiServiceUrl;

    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> analyzeRevenue(List<Map<String, Object>> bookings, LocalDateTime fromDate, LocalDateTime toDate) {
        try {
            String url = aiServiceUrl + "/analyze-revenue";

            Map<String, Object> request = new HashMap<>();
            request.put("bookings", bookings);
            request.put("fromDate", fromDate.toString());
            request.put("toDate", toDate.toString());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return (Map<String, Object>) response.getBody().get("data");
            }

            return Collections.emptyMap();

        } catch (Exception e) {
            log.error("Failed to analyze revenue with AI: {}", e.getMessage());
            return createFallbackAnalysis();
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    public List<Map<String, Object>> optimizeSchedule(
            List<Map<String, Object>> movies,
            List<Map<String, Object>> existingShows,
            List<Integer> rooms,
            Map<String, String> dateRange,
            Map<String, Object> constraints
    ) {
        try {
            String url = aiServiceUrl + "/optimize-schedule";

            Map<String, Object> request = new HashMap<>();
            request.put("movies", movies);
            request.put("existingShows", existingShows);
            request.put("rooms", rooms);
            request.put("dateRange", dateRange);
            request.put("constraints", constraints);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return (List<Map<String, Object>>) response.getBody().get("schedule");
            }

            return Collections.emptyList();

        } catch (Exception e) {
            log.error("Failed to optimize schedule with AI: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    public boolean trainModel(List<Map<String, Object>> historicalBookings) {
        try {
            String url = aiServiceUrl + "/train-model";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<List<Map<String, Object>>> entity = new HttpEntity<>(historicalBookings, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            return response.getStatusCode() == HttpStatus.OK;

        } catch (Exception e) {
            log.error("Failed to train AI model: {}", e.getMessage());
            return false;
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    public Map predictDemand(int hour, int dayOfWeek, boolean isWeekend) {
        try {
            String url = String.format(
                    "%s/predict-demand?hour=%d&day_of_week=%d&is_weekend=%b",
                    aiServiceUrl, hour, dayOfWeek, isWeekend
            );

            ResponseEntity<Map> response = restTemplate.postForEntity(url, null, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody();
            }

            return Collections.emptyMap();

        } catch (Exception e) {
            log.error("Failed to predict demand: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    public boolean isAIServiceAvailable() {
        try {
            String url = aiServiceUrl + "/";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            log.warn("AI service not available: {}", e.getMessage());
            return false;
        }
    }


    private Map<String, Object> createFallbackAnalysis() {
        Map<String, Object> analysis = new HashMap<>();
        analysis.put("insights", Arrays.asList(
                "AI service temporarily unavailable",
                "Using basic analytics"
        ));
        analysis.put("patterns", Collections.emptyMap());
        analysis.put("recommendations", List.of(
                "Enable AI service for advanced insights"
        ));
        return analysis;
    }
    @PreAuthorize("hasRole('ADMIN')")
    public List<Map<String, Object>> prepareBookingsData(LocalDateTime fromDate, LocalDateTime toDate) {
        List<Map<String, Object>> result = new ArrayList<>();
        List<Object[]> rows = bookingRepository.getBookingDataForAI(fromDate,toDate);
        for (Object[] r : rows) {
            Map<String, Object> map = new LinkedHashMap<>();
            Long movieId = (Long) r[3];
            List<Long> genreIds = genreRepository.findGenreIdsByMovie(movieId);
            List<Long> castIds = castRepository.findCastIdsByMovie(movieId);
            map.put("id", r[0]);
            map.put("userId", r[1]);
            map.put("showId", r[2]);
            map.put("movieId", r[3]);
            map.put("movieTitle", r[4]);
            map.put("showDateTime", r[5].toString());
            map.put("totalPrice", r[6]);
            map.put("seatCount", r[7]);
            map.put("bookingDate", r[8].toString());
            map.put("status", r[9]);
            map.put("genreIds",genreIds);
            map.put("castIds",castIds);
            result.add(map);
        }

        return result;
    }
    @PreAuthorize("hasRole('ADMIN')")
    public List<Map<String, Object>> prepareMoviesData() {
        List<Map<String,Object>> result = new ArrayList<>();
        List<Movie> movies = movieRepository.findAll();
        for(Movie movie : movies){
            Map<String,Object> map = new HashMap<>();
            List<Long> castIds = castRepository.findCastIdsByMovie(movie.getId());
            List<Long> genreIds = genreRepository.findGenreIdsByMovie(movie.getId());
            map.put("id",movie.getId());
            map.put("title",movie.getTitle());
            map.put("genreIds",genreIds);
            map.put("castIds",castIds);
            map.put("runtime",movie.getRuntime());
            map.put("popularity",0);
            map.put("voteAverage",movie.getVoteAverage());
            map.put("voteCount", movie.getVoteCount());
            map.put("releaseDate",movie.getReleaseDate().toString());

            result.add(map);
        }
        return result;
    }
    @PreAuthorize("hasRole('ADMIN')")
    public List<Map<String, Object>> prepareShowsData() {
        List<Map<String,Object>> result = new ArrayList<>();
        List<Show> shows = showRepository.findAll();
        for(Show show : shows){
            Map<String,Object> map = new HashMap<>();
            map.put("showId",show.getId());
            map.put("movieId",show.getMovieID().getId());
            map.put("roomId",show.getRoomId().getRoomId());
            map.put("showDateTime",show.getShowDateTime().toString());
            map.put("price",show.getPrice());

            result.add(map);
        }
        return result;
    }
}