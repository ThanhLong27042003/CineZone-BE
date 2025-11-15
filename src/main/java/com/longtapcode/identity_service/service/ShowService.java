package com.longtapcode.identity_service.service;

import com.longtapcode.identity_service.dto.request.ShowRequest;
import com.longtapcode.identity_service.dto.request.UpdateShowRequest;
import com.longtapcode.identity_service.dto.response.ShowResponse;
import com.longtapcode.identity_service.entity.Cast;
import com.longtapcode.identity_service.entity.Genre;
import com.longtapcode.identity_service.entity.Movie;
import com.longtapcode.identity_service.entity.Show;
import com.longtapcode.identity_service.exception.AppException;
import com.longtapcode.identity_service.exception.ErrorCode;
import com.longtapcode.identity_service.mapper.ShowMapper;
import com.longtapcode.identity_service.repository.MovieRepository;
import com.longtapcode.identity_service.repository.ShowRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ShowService {
    ShowRepository showRepository;
    ShowMapper showMapper;
    MovieRepository movieRepository;

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

    public void updateShowById(UpdateShowRequest request, Long id){
        Show show = showRepository.findById(id).orElseThrow(()->new AppException(ErrorCode.SHOW_NOT_EXISTED));
        showMapper.updateShow(show,request);
        show.setMovieID(movieRepository.findById(request.getMovieId()).orElseThrow(()-> new AppException(ErrorCode.MOVIE_NOT_EXISTED)));
        showRepository.save(show);
    }

    public void deleteShowById(Long id){

        showRepository.deleteById(id);
    }

    public ShowResponse createShow(ShowRequest request){
        Movie movie = movieRepository.findById(request.getMovieId()).orElseThrow(()-> new AppException(ErrorCode.MOVIE_NOT_EXISTED));
        showRepository.findByMovieIDAndShowDateTime(movie,request.getShowDateTime()).ifPresent(show-> {
            throw new AppException(ErrorCode.SHOW_EXISTED);
        });
        Show show = showMapper.toShow(request);
        show.setMovieID(movie);
        showRepository.save(show);
        ShowResponse showResponse = showMapper.toShowResponse(show);
        showResponse.setMovieId(show.getMovieID().getId());
        return showResponse;
    }
    public List<ShowResponse> getAllShowByMovieId(Long movieId){
        List<Show> shows = showRepository.findByMovieID(movieRepository.findById(movieId)
                .orElseThrow(()-> new AppException(ErrorCode.MOVIE_NOT_EXISTED)))
                .orElseThrow(()->new AppException(ErrorCode.SHOW_NOT_EXISTED));
        return showMapper.toListShowResponse(shows);
    }
}
