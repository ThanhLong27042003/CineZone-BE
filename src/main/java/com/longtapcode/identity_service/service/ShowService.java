package com.longtapcode.identity_service.service;

import com.longtapcode.identity_service.dto.request.ShowRequest;
import com.longtapcode.identity_service.dto.request.UpdateShowRequest;
import com.longtapcode.identity_service.dto.response.ShowResponse;
import com.longtapcode.identity_service.entity.Movie;
import com.longtapcode.identity_service.entity.Room;
import com.longtapcode.identity_service.entity.Show;
import com.longtapcode.identity_service.exception.AppException;
import com.longtapcode.identity_service.exception.ErrorCode;
import com.longtapcode.identity_service.mapper.ShowMapper;
import com.longtapcode.identity_service.repository.BookingRepository;
import com.longtapcode.identity_service.repository.MovieRepository;
import com.longtapcode.identity_service.repository.RoomRepository;
import com.longtapcode.identity_service.repository.ShowRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cglib.core.Local;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ShowService {
    ShowRepository showRepository;
    ShowMapper showMapper;
    MovieRepository movieRepository;
    RoomRepository roomRepository;
    BookingRepository bookingRepository;

    public ShowResponse getShowById(Long id) {
        Show show = showRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SHOW_NOT_EXISTED));
        return showMapper.toShowResponse(show);
    }

    public List<ShowResponse> getAllShow() {
        List<Show> shows = showRepository.findAll();
        return showMapper.toListShowResponse(shows);
    }

    public List<ShowResponse> getAllShowByMovieId(Long movieId) {
        List<Show> shows = showRepository.findByMovieID(
                movieRepository.findById(movieId)
                        .orElseThrow(() -> new AppException(ErrorCode.MOVIE_NOT_EXISTED))
        ).orElseThrow(() -> new AppException(ErrorCode.SHOW_NOT_EXISTED));
        return showMapper.toListShowResponse(shows);
    }

    // For admin
    public Page<ShowResponse> getAllShowsForAdmin(int page, int size, Long movieId, LocalDateTime dateTime) {
        Page<Show> shows;
        Pageable pageable = PageRequest.of(page, size);

        if (movieId != null && dateTime != null) {
            shows = showRepository.findByMovieIDAndShowDateTime(movieId, dateTime, pageable);
        } else if (movieId != null) {
            shows = showRepository.findByMovieID(movieId, pageable);
        } else if (dateTime != null) {
            shows = showRepository.findByShowDateTime(dateTime, pageable);
        } else {
            shows = showRepository.findAll(pageable);
        }

        return shows.map(showMapper::toShowResponse);
    }

    public void createShow(ShowRequest request) {
        // Validate movie exists
        Movie movie = movieRepository.findById(request.getMovieId())
                .orElseThrow(() -> new AppException(ErrorCode.MOVIE_NOT_EXISTED));

        // Validate room exists
        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION));

        // Calculate end time based on movie runtime
        LocalDateTime startTime = LocalDateTime.of(request.getShowDate(),request.getShowTime());
        LocalDateTime endTime = startTime.plusMinutes(movie.getRuntime() != null ? movie.getRuntime() : 120);

        // Check for overlapping shows in the same room
        boolean hasOverlap = showRepository.countOverlappingShow(
                request.getRoomId(),
                startTime,
                endTime,
                -1L // -1 means no show to exclude (new show)
        ) > 0;

        if (hasOverlap) {
            log.warn("Cannot create show: Overlapping with existing show in room {}", room.getName());
            throw new AppException(ErrorCode.SHOW_OVERLAP); // You might want to create a new error code
        }

        // Create show
        Show show = showMapper.toShow(request);
        show.setMovieID(movie);
        show.setRoomId(room);

        showRepository.save(show);
        log.info("Created show: Movie={}, Room={}, Time={}",
                movie.getTitle(), room.getName(), startTime);

    }

    public void updateShow(Long showId, UpdateShowRequest request) {
        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new AppException(ErrorCode.SHOW_NOT_EXISTED));

        Movie movie = show.getMovieID();
        Room room = show.getRoomId();

        // Update movie if provided
        if (request.getMovieId() != null) {
            movie = movieRepository.findById(request.getMovieId())
                    .orElseThrow(() -> new AppException(ErrorCode.MOVIE_NOT_EXISTED));
        }

        // Update room if provided
        if (request.getRoomId() != null) {
            room = roomRepository.findById(request.getRoomId())
                    .orElseThrow(() -> new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION));
        }

        // Calculate times for overlap check
        LocalDateTime startTime = (request.getShowDate() != null && request.getShowTime() != null) ?
                LocalDateTime.of(request.getShowDate(),request.getShowTime()) : show.getShowDateTime();
        LocalDateTime endTime = startTime.plusMinutes(
                movie.getRuntime() != null ? movie.getRuntime() : 120);

        boolean hasOverlap = showRepository.countOverlappingShow(
                room.getRoomId(),
                startTime,
                endTime,
                showId
        )>0;

        if (hasOverlap) {
            log.warn("Cannot update show {}: Overlapping with existing show in room {}",
                    showId, room.getName());
            throw new AppException(ErrorCode.SHOW_OVERLAP);
        }

        // Update show
        showMapper.updateShow(show, request);
        show.setMovieID(movie);
        show.setRoomId(room);

        showRepository.save(show);
        log.info("Updated show {}: Movie={}, Room={}, Time={}",
                showId, movie.getTitle(), room.getName(), startTime);
    }

    public void deleteShow(Long showId) {
        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new AppException(ErrorCode.SHOW_NOT_EXISTED));

        showRepository.delete(show);
        log.info("Deleted show: {}", showId);
    }

    public List<ShowResponse> getShowsByRoomAndDateRange(
            Long roomId,
            LocalDateTime startDate,
            LocalDateTime endDate) {

        if (!roomRepository.existsById(roomId)) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }

        List<Show> shows = showRepository.findByRoomAndDateRange(roomId, startDate, endDate);
        return showMapper.toListShowResponse(shows);
    }
}
