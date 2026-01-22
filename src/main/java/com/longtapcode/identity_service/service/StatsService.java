package com.longtapcode.identity_service.service;

import com.longtapcode.identity_service.entity.Movie;
import com.longtapcode.identity_service.entity.Show;
import com.longtapcode.identity_service.repository.BookingRepository;
import com.longtapcode.identity_service.repository.MovieRepository;
import com.longtapcode.identity_service.repository.ShowRepository;
import com.longtapcode.identity_service.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class StatsService {

    MovieRepository movieRepository;
    ShowRepository showRepository;
    BookingRepository bookingRepository;
    StringRedisTemplate redisTemplate;


    public Map<String, Object> getCinemaStats() {
        Map<String, Object> stats = new HashMap<>();

        long nowShowingCount = movieRepository.count();
        stats.put("nowShowing", nowShowingCount);

        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfMonth = LocalDateTime.now();
        Long totalBooking = bookingRepository.countByDateRange(startOfMonth, endOfMonth);
        stats.put("totalBooking", totalBooking);

        List<Movie> allMovies = movieRepository.findAll();
        double avgRating = allMovies.stream()
                .filter(m -> m.getVoteAverage() != null)
                .mapToDouble(m -> m.getVoteAverage().doubleValue())
                .average()
                .orElse(0.0);
        stats.put("averageRating", Math.round(avgRating * 10) / 10.0);

        // Total reviews
        Long totalReviews = allMovies.stream()
                .filter(m -> m.getVoteCount() != null)
                .mapToLong(Movie::getVoteCount)
                .sum();
        stats.put("totalReviews", totalReviews);

        // Tickets sold this month
        Long ticketsSold = bookingRepository.countByStatusAndDateRange("CONFIRMED", startOfMonth, endOfMonth);
        stats.put("ticketsSoldThisMonth", ticketsSold);

        return stats;
    }

    public List<Map<String, Object>> getTrendingMovies() {
        LocalDateTime today = LocalDateTime.now();
        LocalDateTime endOfDay = LocalDateTime.now().withYear(2028);

        Pageable pageable = PageRequest.of(0,10);
        LocalDateTime last7Days = today.minusDays(7);
        List<Object[]> topMoviesData = bookingRepository.getTopMoviesByBookings(last7Days, today, pageable);
        List<Map<String, Object>> trendingMovies = new ArrayList<>();

        for (Object[] data : topMoviesData) {
            String movieTitle = (String) data[0];
            Long bookingCount = ((Number) data[1]).longValue();

            // Find movie by title
            Optional<Movie> movieOpt = movieRepository.findByTitle(movieTitle);
            if (movieOpt.isEmpty()) continue;

            Movie movie = movieOpt.get();

            // Get today's shows for this movie
            List<Show> todayShows = showRepository.findByRoomAndDateRange(
                            null,
                            today,
                            endOfDay
                    ).stream()
                    .filter(s -> s.getMovieID().getId().equals(movie.getId()))
                    .filter(s -> s.getShowDateTime().isAfter(today)) // only future shows
                    .sorted(Comparator.comparing(Show::getShowDateTime))
                    .limit(4)
                    .toList();
            log.info(todayShows.toString());
            if (todayShows.isEmpty()) continue;

            // Format showtimes
            String showtime = todayShows.stream()
                    .map(show -> show.getShowDateTime().format(DateTimeFormatter.ofPattern("HH:mm")))
                    .collect(Collectors.joining(" • "));

            // Calculate available seats for first show
            Show firstShow = todayShows.getFirst();
            int availableSeats = calculateAvailableSeats(firstShow.getId());

            // Determine trend
            String trend = determineTrend(bookingCount);
            String trendPercent = calculateTrendPercent(bookingCount);

            Map<String, Object> movieData = new HashMap<>();
            movieData.put("id", movie.getId());
            movieData.put("title", movie.getTitle());
            movieData.put("poster", movie.getPosterPath());
            movieData.put("rating", movie.getVoteAverage());
            movieData.put("showtime", showtime);
            movieData.put("badge", determineBadge(movie, bookingCount));
            movieData.put("seats", availableSeats + " seats left");
            movieData.put("trend", trend);
            movieData.put("trendPercent", trendPercent);
            movieData.put("showId", firstShow.getId());

            trendingMovies.add(movieData);
        }
        log.info(trendingMovies.toString());
        return trendingMovies;
    }


    private int calculateAvailableSeats(Long showId) {
        Set<String> heldKeys = redisTemplate.keys("hold:" + showId + ":*");
        Set<String> bookedKeys = redisTemplate.keys("booked:" + showId + ":*");

        int totalSeats = 60; // Assuming 60 seats per room
        int occupiedSeats = (heldKeys != null ? heldKeys.size() : 0) +
                (bookedKeys != null ? bookedKeys.size() : 0);

        return Math.max(0, totalSeats - occupiedSeats);
    }


    private String determineTrend(Long bookingCount) {
        if (bookingCount > 50) return "up";
        if (bookingCount < 20) return "down";
        return "stable";
    }


    private String calculateTrendPercent(Long bookingCount) {
        if (bookingCount > 50) return "+" + (bookingCount / 2) + "%";
        if (bookingCount < 20) return "-" + (30 - bookingCount) + "%";
        return "0%";
    }


    private String determineBadge(Movie movie, Long bookingCount) {
        if (bookingCount > 50) return "Hot";

        LocalDate releaseDate = movie.getReleaseDate();
        if (releaseDate != null && releaseDate.isAfter(LocalDate.now().minusMonths(1))) {
            return "New";
        }

        if (movie.getVoteAverage() != null && movie.getVoteAverage().doubleValue() > 8.0) {
            return "IMAX";
        }

        return "Hot";
    }


    public List<Map<String, Object>> getComingSoonMovies() {
        LocalDate today = LocalDate.now();
        LocalDate futureDate = today.plusMonths(2);

        List<Movie> upcomingMovies = movieRepository.findAll().stream()
                .filter(m -> m.getReleaseDate() != null)
                .filter(m -> m.getReleaseDate().isAfter(today) && m.getReleaseDate().isBefore(futureDate))
                .sorted(Comparator.comparing(Movie::getReleaseDate))
                .limit(4)
                .collect(Collectors.toList());

        List<Map<String, Object>> result = new ArrayList<>();

        for (Movie movie : upcomingMovies) {
            Map<String, Object> movieData = new HashMap<>();
            movieData.put("id", movie.getId());
            movieData.put("title", movie.getTitle());
            movieData.put("date", movie.getReleaseDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
            movieData.put("genre", movie.getGenres().isEmpty() ? "Unknown" :
                    movie.getGenres().iterator().next().getName());
            movieData.put("preOrder", movie.getReleaseDate().isBefore(today.plusMonths(1)));

            result.add(movieData);
        }

        return result;
    }

    public Map<String, Object> getCinemaInfo() {
        Map<String, Object> info = new HashMap<>();

        info.put("name", "CineZone Theater");
        info.put("address", "123 Nguyen Hue St, District 1, HCMC");
        info.put("screens", 6);
        info.put("seats", 720);
        info.put("facilities", Arrays.asList("4DX", "IMAX", "Dolby Atmos", "VIP Lounge"));
        info.put("openHours", "08:00 AM - 11:30 PM");
        info.put("parking", "Free 3 hours");

        return info;
    }


    public Map<String, Object> getAllStats() {
        Map<String, Object> allStats = new HashMap<>();

        allStats.put("stats", getCinemaStats());
        allStats.put("trendingMovies", getTrendingMovies());
        allStats.put("comingSoon", getComingSoonMovies());
        allStats.put("cinemaInfo", getCinemaInfo());

        return allStats;
    }
}