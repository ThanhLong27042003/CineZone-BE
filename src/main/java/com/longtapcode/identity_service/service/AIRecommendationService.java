package com.longtapcode.identity_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.longtapcode.identity_service.entity.Movie;
import com.longtapcode.identity_service.repository.CastRepository;
import com.longtapcode.identity_service.repository.GenreRepository;
import com.longtapcode.identity_service.repository.MovieRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
public class AIRecommendationService {

    final RestTemplate restTemplate = new RestTemplate();
    final ObjectMapper objectMapper;
    final MovieRepository movieRepository;
    final GenreRepository genreRepository;
    final CastRepository castRepository;

    @Value("${ai.service.url:http://localhost:8000}")
    String aiServiceUrl;

    /**
     * Get AI-powered recommendations using BERT and content-based filtering
     */
    public List<Movie> getAIRecommendations(Long movieId, int limit, boolean useCollaborative, List<Integer> userHistory) {
        try {
            log.info("🤖 Calling AI service for movie recommendations: movieId={}, limit={}", movieId, limit);

            // Check if AI service is available
            if (!isAIServiceAvailable()) {
                log.warn("⚠️ AI service not available, falling back to simple recommendations");
                return getFallbackRecommendations(movieId, limit);
            }

            // Prepare all movies data for AI service
            List<Map<String, Object>> allMoviesData = prepareMoviesDataForAI();

            // Build request
            Map<String, Object> request = new HashMap<>();
            request.put("movieId", movieId);
            request.put("allMovies", allMoviesData);
            request.put("limit", limit);
            request.put("useCollaborative", useCollaborative);
            request.put("userHistory", userHistory != null ? userHistory : Collections.emptyList());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            // Call AI service
            String url = aiServiceUrl + "/recommendations";
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();

                List<Map<String, Object>> recommendations =
                        (List<Map<String, Object>>) responseBody.get("recommendations");

                String algorithm = (String) responseBody.get("algorithm");
                Double processingTime = ((Number) responseBody.get("processingTime")).doubleValue();

                log.info("✅ AI recommendations received: count={}, algorithm={}, time={:.2f}s",
                        recommendations.size(), algorithm, processingTime);

                // Convert to Movie entities
                return recommendations.stream()
                        .map(rec -> {
                            Integer recMovieId = (Integer) rec.get("movieId");
                            return movieRepository.findById(recMovieId.longValue()).orElse(null);
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
            }

            log.warn("⚠️ AI service returned empty response, using fallback");
            return getFallbackRecommendations(movieId, limit);

        } catch (Exception e) {
            log.error("❌ Failed to get AI recommendations: {}", e.getMessage());
            return getFallbackRecommendations(movieId, limit);
        }
    }

    /**
     * Prepare movie data in format expected by AI service
     */
    private List<Map<String, Object>> prepareMoviesDataForAI() {
        List<Movie> allMovies = movieRepository.findAll();
        List<Map<String, Object>> moviesData = new ArrayList<>();

        for (Movie movie : allMovies) {
            Map<String, Object> movieData = new HashMap<>();

            movieData.put("id", movie.getId());
            movieData.put("title", movie.getTitle());
            movieData.put("overview", movie.getOverview() != null ? movie.getOverview() : "");
            movieData.put("releaseDate", movie.getReleaseDate() != null ? movie.getReleaseDate().toString() : null);
            movieData.put("runtime", movie.getRuntime());
            movieData.put("voteAverage", movie.getVoteAverage() != null ? movie.getVoteAverage().doubleValue() : null);
            movieData.put("voteCount", movie.getVoteCount());

            // Get genre IDs
            List<Long> genreIds = genreRepository.findGenreIdsByMovie(movie.getId());
            movieData.put("genreIds", genreIds);

            // Get cast IDs
            List<Long> castIds = castRepository.findCastIdsByMovie(movie.getId());
            movieData.put("castIds", castIds);

            moviesData.add(movieData);
        }

        return moviesData;
    }

    /**
     * Check if AI service is available
     */
    private boolean isAIServiceAvailable() {
        try {
            String url = aiServiceUrl + "/health";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            log.warn("AI service health check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Fallback recommendations using simple similarity
     * (Same as the original implementation)
     */
    private List<Movie> getFallbackRecommendations(Long movieId, int limit) {
        log.info("Using fallback recommendation algorithm");

        Optional<Movie> currentMovieOpt = movieRepository.findById(movieId);
        if (currentMovieOpt.isEmpty()) {
            return movieRepository.findAll().stream()
                    .limit(limit)
                    .collect(Collectors.toList());
        }

        Movie currentMovie = currentMovieOpt.get();
        List<Movie> allMovies = movieRepository.findAll();

        // Calculate similarity scores
        Map<Movie, Double> similarityScores = new HashMap<>();

        for (Movie movie : allMovies) {
            if (movie.getId().equals(movieId)) continue;

            double score = calculateSimpleSimilarity(currentMovie, movie);
            similarityScores.put(movie, score);
        }

        // Sort by similarity score and return top N
        return similarityScores.entrySet().stream()
                .sorted(Map.Entry.<Movie, Double>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * Simple similarity calculation (fallback)
     */
    private double calculateSimpleSimilarity(Movie movie1, Movie movie2) {
        double score = 0.0;

        // Genre similarity (40% weight)
        Set<Long> genres1 = movie1.getGenres().stream().map(g -> g.getId()).collect(Collectors.toSet());
        Set<Long> genres2 = movie2.getGenres().stream().map(g -> g.getId()).collect(Collectors.toSet());

        Set<Long> intersection = new HashSet<>(genres1);
        intersection.retainAll(genres2);

        Set<Long> union = new HashSet<>(genres1);
        union.addAll(genres2);

        if (!union.isEmpty()) {
            score += (intersection.size() / (double) union.size()) * 0.4;
        }

        // Cast similarity (30% weight)
        Set<Long> cast1 = movie1.getCasts().stream().map(c -> c.getId()).collect(Collectors.toSet());
        Set<Long> cast2 = movie2.getCasts().stream().map(c -> c.getId()).collect(Collectors.toSet());

        Set<Long> castIntersection = new HashSet<>(cast1);
        castIntersection.retainAll(cast2);

        Set<Long> castUnion = new HashSet<>(cast1);
        castUnion.addAll(cast2);

        if (!castUnion.isEmpty()) {
            score += (castIntersection.size() / (double) castUnion.size()) * 0.3;
        }

        // Rating similarity (20% weight)
        if (movie1.getVoteAverage() != null && movie2.getVoteAverage() != null) {
            double ratingDiff = Math.abs(movie1.getVoteAverage().doubleValue() -
                    movie2.getVoteAverage().doubleValue());
            score += (1 - ratingDiff / 10.0) * 0.2;
        }

        // Release date similarity (10% weight)
        if (movie1.getReleaseDate() != null && movie2.getReleaseDate() != null) {
            long daysDiff = Math.abs(movie1.getReleaseDate().toEpochDay() -
                    movie2.getReleaseDate().toEpochDay());
            double yearsDiff = daysDiff / 365.0;
            score += Math.max(0, (1 - yearsDiff / 10.0)) * 0.1;
        }

        return score;
    }

    /**
     * Get recommendations with optional user history for collaborative filtering
     */
    public List<Movie> getRecommendations(Long movieId, int limit) {
        return getAIRecommendations(movieId, limit, false, null);
    }

    /**
     * Get recommendations with user history (hybrid approach)
     */
    public List<Movie> getRecommendationsWithHistory(Long movieId, int limit, List<Integer> userHistory) {
        return getAIRecommendations(movieId, limit, true, userHistory);
    }


}