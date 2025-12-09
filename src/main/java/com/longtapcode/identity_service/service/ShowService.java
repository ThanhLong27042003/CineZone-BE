package com.longtapcode.identity_service.service;

import com.longtapcode.identity_service.dto.request.ShowRequest;
import com.longtapcode.identity_service.dto.request.UpdateShowRequest;
import com.longtapcode.identity_service.dto.response.ShowResponse;
import com.longtapcode.identity_service.entity.Movie;
import com.longtapcode.identity_service.entity.Show;
import com.longtapcode.identity_service.exception.AppException;
import com.longtapcode.identity_service.exception.ErrorCode;
import com.longtapcode.identity_service.mapper.ShowMapper;
import com.longtapcode.identity_service.repository.BookingRepository;
import com.longtapcode.identity_service.repository.MovieRepository;
import com.longtapcode.identity_service.repository.ShowRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
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
    private final BookingRepository bookingRepository;
    private final SeatService seatService;

    public ShowResponse getShowById(Long id){
        Show show = showRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.SHOW_NOT_EXISTED));
        ShowResponse showResponse = showMapper.toShowResponse(show);
        showResponse.setMovieId(show.getMovieID().getId());
        return showResponse;
    }
    public List<ShowResponse> getAllShow(){
        List<Show> shows = showRepository.findAll();
        return showMapper.toListShowResponse(shows);
    }


    public List<ShowResponse> getAllShowByMovieId(Long movieId){
        List<Show> shows = showRepository.findByMovieID(movieRepository.findById(movieId)
                .orElseThrow(()-> new AppException(ErrorCode.MOVIE_NOT_EXISTED)))
                .orElseThrow(()->new AppException(ErrorCode.SHOW_NOT_EXISTED));
        return showMapper.toListShowResponse(shows);
    }


    //For admin
    public Page<ShowResponse> getAllShowsForAdmin(int page,int size, Long movieId, LocalDateTime dateTime) {
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

    public ShowResponse createShow(ShowRequest request) {
        Movie movie = movieRepository.findById(request.getMovieId())
                .orElseThrow(() -> new AppException(ErrorCode.MOVIE_NOT_EXISTED));

        Show show = showMapper.toShow(request);
        show.setMovieID(movie);

        return showMapper.toShowResponse(showRepository.save(show));
    }

    public ShowResponse updateShow(Long showId, UpdateShowRequest request) {
        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new AppException(ErrorCode.SHOW_NOT_EXISTED));

        if (request.getMovieId() != null) {
            Movie movie = movieRepository.findById(request.getMovieId())
                    .orElseThrow(() -> new AppException(ErrorCode.MOVIE_NOT_EXISTED));
            show.setMovieID(movie);
        }

        showMapper.updateShow(show, request);
        return showMapper.toShowResponse(showRepository.save(show));
    }

    public void deleteShow(Long showId) {
        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new AppException(ErrorCode.SHOW_NOT_EXISTED));

//        if (bookingRepository.existsByShow(show)) {
//            throw new AppException(ErrorCode.SHOW_HAS_BOOKINGS);
//        }

        showRepository.delete(show);
    }
}
