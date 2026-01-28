//package com.longtapcode.identity_service.service;
//
//import com.longtapcode.identity_service.dto.request.ShowRequest;
//import com.longtapcode.identity_service.dto.request.UpdateShowRequest;
//import com.longtapcode.identity_service.dto.response.ShowResponse;
//import com.longtapcode.identity_service.entity.Movie;
//import com.longtapcode.identity_service.entity.Room;
//import com.longtapcode.identity_service.entity.Show;
//import com.longtapcode.identity_service.exception.AppException;
//import com.longtapcode.identity_service.exception.ErrorCode;
//import com.longtapcode.identity_service.mapper.ShowMapper;
//import com.longtapcode.identity_service.repository.BookingRepository;
//import com.longtapcode.identity_service.repository.MovieRepository;
//import com.longtapcode.identity_service.repository.RoomRepository;
//import com.longtapcode.identity_service.repository.ShowRepository;
//import lombok.AccessLevel;
//import lombok.RequiredArgsConstructor;
//import lombok.experimental.FieldDefaults;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.cglib.core.Local;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.stereotype.Service;
//
//import java.time.LocalDateTime;
//import java.util.List;
//
//@Service
//@RequiredArgsConstructor
//@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
//@Slf4j
//public class ShowService {
//    ShowRepository showRepository;
//    ShowMapper showMapper;
//    MovieRepository movieRepository;
//    RoomRepository roomRepository;
//    BookingRepository bookingRepository;
//
//    public ShowResponse getShowById(Long id) {
//        Show show = showRepository.findById(id)
//                .orElseThrow(() -> new AppException(ErrorCode.SHOW_NOT_EXISTED));
//        return showMapper.toShowResponse(show);
//    }
//
//    public List<ShowResponse> getAllShow() {
//        List<Show> shows = showRepository.findAll();
//        return showMapper.toListShowResponse(shows);
//    }
//
//    public List<ShowResponse> getAllShowByMovieId(Long movieId) {
//        List<Show> shows = showRepository.findByMovieID(
//                movieRepository.findById(movieId)
//                        .orElseThrow(() -> new AppException(ErrorCode.MOVIE_NOT_EXISTED))
//        ).orElseThrow(() -> new AppException(ErrorCode.SHOW_NOT_EXISTED));
//        return showMapper.toListShowResponse(shows);
//    }
//
//    @PreAuthorize("hasRole('ADMIN')")
//    public Page<ShowResponse> getAllShowsForAdmin(int page, int size, Long movieId, LocalDateTime dateTime) {
//        Page<Show> shows;
//        Pageable pageable = PageRequest.of(page, size);
//
//        if (movieId != null && dateTime != null) {
//            shows = showRepository.findByMovieIDAndShowDateTime(movieId, dateTime, pageable);
//        } else if (movieId != null) {
//            shows = showRepository.findByMovieID(movieId, pageable);
//        } else if (dateTime != null) {
//            shows = showRepository.findByShowDateTime(dateTime, pageable);
//        } else {
//            shows = showRepository.findAll(pageable);
//        }
//
//        return shows.map(showMapper::toShowResponse);
//    }
//    @PreAuthorize("hasRole('ADMIN')")
//    public void createShow(ShowRequest request) {
//        // Validate movie exists
//        Movie movie = movieRepository.findById(request.getMovieId())
//                .orElseThrow(() -> new AppException(ErrorCode.MOVIE_NOT_EXISTED));
//
//        // Validate room exists
//        Room room = roomRepository.findById(request.getRoomId())
//                .orElseThrow(() -> new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION));
//
//        // Calculate end time based on movie runtime
//        LocalDateTime startTime = LocalDateTime.of(request.getShowDate(),request.getShowTime());
//        LocalDateTime endTime = startTime.plusMinutes(movie.getRuntime() != null ? movie.getRuntime() : 120);
//
//        // Check for overlapping shows in the same room
//        boolean hasOverlap = showRepository.countOverlappingShow(
//                request.getRoomId(),
//                startTime,
//                endTime,
//                -1L // -1 means no show to exclude (new show)
//        ) > 0;
//
//        if (hasOverlap) {
//            log.warn("Cannot create show: Overlapping with existing show in room {}", room.getName());
//            throw new AppException(ErrorCode.SHOW_OVERLAP); // You might want to create a new error code
//        }
//
//        // Create show
//        Show show = showMapper.toShow(request);
//        show.setMovieID(movie);
//        show.setRoomId(room);
//
//        showRepository.save(show);
//        log.info("Created show: Movie={}, Room={}, Time={}",
//                movie.getTitle(), room.getName(), startTime);
//
//    }
//    @PreAuthorize("hasRole('ADMIN')")
//    public void updateShow(Long showId, UpdateShowRequest request) {
//        Show show = showRepository.findById(showId)
//                .orElseThrow(() -> new AppException(ErrorCode.SHOW_NOT_EXISTED));
//
//        Movie movie = show.getMovieID();
//        Room room = show.getRoomId();
//
//        // Update movie if provided
//        if (request.getMovieId() != null) {
//            movie = movieRepository.findById(request.getMovieId())
//                    .orElseThrow(() -> new AppException(ErrorCode.MOVIE_NOT_EXISTED));
//        }
//
//        // Update room if provided
//        if (request.getRoomId() != null) {
//            room = roomRepository.findById(request.getRoomId())
//                    .orElseThrow(() -> new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION));
//        }
//
//        // Calculate times for overlap check
//        LocalDateTime startTime = (request.getShowDate() != null && request.getShowTime() != null) ?
//                LocalDateTime.of(request.getShowDate(),request.getShowTime()) : show.getShowDateTime();
//        LocalDateTime endTime = startTime.plusMinutes(
//                movie.getRuntime() != null ? movie.getRuntime() : 120);
//
//        boolean hasOverlap = showRepository.countOverlappingShow(
//                room.getRoomId(),
//                startTime,
//                endTime,
//                showId
//        )>0;
//
//        if (hasOverlap) {
//            log.warn("Cannot update show {}: Overlapping with existing show in room {}",
//                    showId, room.getName());
//            throw new AppException(ErrorCode.SHOW_OVERLAP);
//        }
//
//        // Update show
//        showMapper.updateShow(show, request);
//        show.setMovieID(movie);
//        show.setRoomId(room);
//
//        showRepository.save(show);
//        log.info("Updated show {}: Movie={}, Room={}, Time={}",
//                showId, movie.getTitle(), room.getName(), startTime);
//    }
//    @PreAuthorize("hasRole('ADMIN')")
//    public void deleteShow(Long showId) {
//        Show show = showRepository.findById(showId)
//                .orElseThrow(() -> new AppException(ErrorCode.SHOW_NOT_EXISTED));
//
//        showRepository.delete(show);
//        log.info("Deleted show: {}", showId);
//    }
//
//    public List<ShowResponse> getShowsByRoomAndDateRange(
//            Long roomId,
//            LocalDateTime startDate,
//            LocalDateTime endDate) {
//
//        if (!roomRepository.existsById(roomId)) {
//            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
//        }
//
//        List<Show> shows = showRepository.findByRoomAndDateRange(roomId, startDate, endDate);
//        return showMapper.toListShowResponse(shows);
//    }
//}
package com.longtapcode.identity_service.service;

import com.longtapcode.identity_service.dto.request.ShowRequest;
import com.longtapcode.identity_service.dto.request.UpdateShowRequest;
import com.longtapcode.identity_service.dto.response.ShowResponse;
import com.longtapcode.identity_service.entity.*;
import com.longtapcode.identity_service.exception.AppException;
import com.longtapcode.identity_service.exception.ErrorCode;
import com.longtapcode.identity_service.mapper.ShowMapper;
import com.longtapcode.identity_service.repository.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ShowService {
    ShowRepository showRepository;
    MovieRepository movieRepository;
    RoomRepository roomRepository;
    ShowMapper showMapper;
    BookingRepository bookingRepository;

    // ==================== USER METHODS - CHỈ LẤY SHOW CHƯA CHIẾU ====================

    public List<ShowResponse> getAllShow() {
        LocalDateTime now = LocalDateTime.now();
        List<Show> shows = showRepository.findAll();

        List<Show> upcomingShows = shows.stream()
                .filter(show -> show.getShowDateTime().isAfter(now))
                .collect(Collectors.toList());

        return showMapper.toListShowResponse(upcomingShows);
    }

    public ShowResponse getShowById(Long id) {
        LocalDateTime now = LocalDateTime.now();
        Show show = showRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SHOW_NOT_EXISTED));

        // Kiểm tra nếu show đã chiếu
        if (show.getShowDateTime().isBefore(now)) {
            throw new AppException(ErrorCode.SHOW_ALREADY_PASSED);
        }

        return showMapper.toShowResponse(show);
    }

    public List<ShowResponse> getAllShowByMovieId(Long movieId) {
        LocalDateTime now = LocalDateTime.now();
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new AppException(ErrorCode.MOVIE_NOT_EXISTED));

        List<Show> shows = showRepository.findByMovieID(movie)
                .orElseThrow(() -> new AppException(ErrorCode.SHOW_NOT_EXISTED));

        // Chỉ lấy show chưa chiếu
        List<Show> upcomingShows = shows.stream()
                .filter(show -> show.getShowDateTime().isAfter(now))
                .sorted(Comparator.comparing(Show::getShowDateTime))
                .collect(Collectors.toList());

        return showMapper.toListShowResponse(upcomingShows);
    }

    // ==================== ADMIN METHODS - LẤY TẤT CẢ SHOW ====================

    @PreAuthorize("hasRole('ADMIN')")
    public Page<ShowResponse> getAllShowsForAdmin(int page, int size, Long movieId, LocalDateTime dateTime) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Show> shows;

        if (movieId != null && dateTime != null) {
            shows = showRepository.findByMovieID_IdAndShowDateTime(movieId, dateTime, pageable);
        } else if (movieId != null) {
            shows = showRepository.findByMovieID_Id(movieId, pageable);
        } else if (dateTime != null) {
            shows = showRepository.findByShowDateTime(dateTime, pageable);
        } else {
            shows = showRepository.findAll(pageable);
        }

        List<ShowResponse> responses = showMapper.toListShowResponse(shows.getContent());

        // Thêm thông tin trạng thái show cho admin
        LocalDateTime now = LocalDateTime.now();
        responses.forEach(response -> {
            Show show = shows.getContent().stream()
                    .filter(s -> s.getId().equals(response.getShowId()))
                    .findFirst()
                    .orElse(null);

            if (show != null) {
                response.setShowStatus(getShowStatus(show.getShowDateTime(), now));
            }
        });

        return new PageImpl<>(responses, pageable, shows.getTotalElements());
    }

    // Helper method để xác định trạng thái show
    private String getShowStatus(LocalDateTime showDateTime, LocalDateTime now) {
        if (showDateTime.isBefore(now)) {
            return "COMPLETED"; // Đã chiếu
        } else if (showDateTime.isBefore(now.plusHours(2))) {
            return "ONGOING"; // Đang chiếu (trong vòng 2h tới)
        } else {
            return "UPCOMING"; // Sắp chiếu
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void createShow(ShowRequest request) {
        Movie movie = movieRepository.findById(request.getMovieId())
                .orElseThrow(() -> new AppException(ErrorCode.MOVIE_NOT_EXISTED));

        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_EXISTED));

        LocalDateTime showDateTime = LocalDateTime.of(request.getShowDate(), request.getShowTime());

        // Không cho phép tạo show trong quá khứ
        if (showDateTime.isBefore(LocalDateTime.now())) {
            throw new AppException(ErrorCode.INVALID_SHOW_TIME);
        }

        // Kiểm tra overlap
        LocalDateTime startDateTime = showDateTime;
        LocalDateTime endDateTime = showDateTime.plusMinutes(movie.getRuntime());

        Long overlappingCount = showRepository.countOverlappingShow(
                request.getRoomId(),
                startDateTime,
                endDateTime,
                null
        );

        if (overlappingCount > 0) {
            throw new AppException(ErrorCode.SHOW_OVERLAP);
        }

        Show show = showMapper.toShow(request);
        show.setMovieID(movie);
        show.setRoomId(room);
        showRepository.save(show);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void updateShow(Long showId, UpdateShowRequest request) {
        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new AppException(ErrorCode.SHOW_NOT_EXISTED));

        boolean hasBookings = bookingRepository.existsByShowID(show);
        if (hasBookings) {
            throw new AppException(ErrorCode.SHOW_HAS_BOOKINGS);
        }

        Movie movie = movieRepository.findById(request.getMovieId())
                .orElseThrow(() -> new AppException(ErrorCode.MOVIE_NOT_EXISTED));

        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_EXISTED));

        LocalDateTime newShowDateTime = LocalDateTime.of(request.getShowDate(), request.getShowTime());

        // Kiểm tra overlap
        LocalDateTime startDateTime = newShowDateTime;
        LocalDateTime endDateTime = newShowDateTime.plusMinutes(movie.getRuntime());

        Long overlappingCount = showRepository.countOverlappingShow(
                request.getRoomId(),
                startDateTime,
                endDateTime,
                showId
        );

        if (overlappingCount > 0) {
            throw new AppException(ErrorCode.SHOW_OVERLAP);
        }

        showMapper.updateShow(show, request);
        show.setMovieID(movie);
        show.setRoomId(room);
        showRepository.save(show);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void deleteShow(Long showId) {
        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new AppException(ErrorCode.SHOW_NOT_EXISTED));

        boolean hasBookings = bookingRepository.existsByShowID(show);
        if (hasBookings) {
            throw new AppException(ErrorCode.SHOW_HAS_BOOKINGS);
        }

        showRepository.deleteById(showId);
    }

    /**
     * Lấy tất cả suất chiếu theo ngày, nhóm theo phim
     */
    public Map<String, List<ShowResponse>> getShowsByDate(LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);

        List<Show> shows = showRepository.findByRoomAndDateRange(
                null,
                startOfDay,
                endOfDay
        );

        // Lọc chỉ lấy show chưa chiếu
        List<Show> upcomingShows = shows.stream()
                .filter(show -> show.getShowDateTime().isAfter(LocalDateTime.now()))
                .sorted(Comparator.comparing(Show::getShowDateTime))
                .collect(Collectors.toList());

        // Nhóm theo phim
        Map<String, List<ShowResponse>> groupedByMovie = upcomingShows.stream()
                .map(showMapper::toShowResponse)
                .collect(Collectors.groupingBy(
                        response -> {
                            Show show = shows.stream()
                                    .filter(s -> s.getId().equals(response.getShowId()))
                                    .findFirst()
                                    .orElse(null);
                            return show != null ? show.getMovieID().getTitle() : "Unknown";
                        }
                ));

        log.info("Found {} movies with shows on {}", groupedByMovie.size(), date);
        return groupedByMovie;
    }

    /**
     * Lấy danh sách các ngày có suất chiếu trong 30 ngày tới
     */
    public List<LocalDate> getAvailableDates(Long movieId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime futureDate = now.plusDays(30);

        List<Show> shows;
        if (movieId != null) {
            Movie movie = movieRepository.findById(movieId)
                    .orElseThrow(() -> new AppException(ErrorCode.MOVIE_NOT_EXISTED));
            shows = showRepository.findByMovieID(movie)
                    .orElse(Collections.emptyList());
        } else {
            shows = showRepository.findByRoomAndDateRange(null, now, futureDate);
        }

        // Lấy danh sách ngày duy nhất
        Set<LocalDate> uniqueDates = shows.stream()
                .filter(show -> show.getShowDateTime().isAfter(now))
                .map(show -> show.getShowDateTime().toLocalDate())
                .collect(Collectors.toSet());

        List<LocalDate> sortedDates = new ArrayList<>(uniqueDates);
        Collections.sort(sortedDates);

        log.info("Found {} available dates", sortedDates.size());
        return sortedDates;
    }
}