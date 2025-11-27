package com.longtapcode.identity_service.service;

import com.longtapcode.identity_service.dto.request.MovieRequest;
import com.longtapcode.identity_service.dto.request.UpdateMovieRequest;
import com.longtapcode.identity_service.dto.response.MovieResponse;
import com.longtapcode.identity_service.entity.Cast;
import com.longtapcode.identity_service.entity.Genre;
import com.longtapcode.identity_service.entity.Movie;
import com.longtapcode.identity_service.entity.User;
import com.longtapcode.identity_service.exception.AppException;
import com.longtapcode.identity_service.exception.ErrorCode;
import com.longtapcode.identity_service.mapper.MovieMapper;
import com.longtapcode.identity_service.repository.CastRepository;
import com.longtapcode.identity_service.repository.GenreRepository;
import com.longtapcode.identity_service.repository.MovieRepository;
import com.longtapcode.identity_service.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class MovieService {
    MovieRepository movieRepository;
    MovieMapper movieMapper;
    CastRepository castRepository;
    GenreRepository genreRepository;
    private final UserRepository userRepository;

    public MovieResponse getMovieById(Long id){
        Movie movie = movieRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.MOVIE_NOT_EXISTED));
        return movieMapper.toMovieResponse(movie);
    }
    public List<MovieResponse> getAllMovie(){
        List<Movie> listMovie = movieRepository.findAll();
        return movieMapper.toListMovieResponse(listMovie);
    }

    public void updateMovieById(UpdateMovieRequest request,Long id){
        Movie movie = movieRepository.findById(id).orElseThrow(()->new AppException(ErrorCode.MOVIE_NOT_EXISTED));
        movieMapper.updateMovie(movie,request);
        movieRepository.save(movie);
    }

    public void deleteMovieById(Long id){

        movieRepository.deleteById(id);
    }

    public MovieResponse createMovie(MovieRequest request){
        movieRepository.findByTitle(request.getTitle()).ifPresent(movie-> {
            throw new AppException(ErrorCode.MOVIE_EXISTED);
        });
        Movie movie = movieMapper.toMovie(request);
        Set<Long> castIds = request.getCasts().stream()
                .map(Cast::getId)
                .collect(Collectors.toSet());
        Set<Long> genreIds = request.getGenres().stream()
                .map(Genre::getId)
                .collect(Collectors.toSet());
        Set<Cast> casts = new HashSet<>(castRepository.findAllById(castIds));
        Set<Genre> genres = new HashSet<>(genreRepository.findAllById(genreIds));
        movie.setCasts(casts);
        movie.setGenres(genres);
        movieRepository.save(movie);
        return movieMapper.toMovieResponse(movie);
    }

    public List<List<MovieResponse>> getTopMovieForHomePage(List<String> genres){
        List<List<MovieResponse>> homePageMovieList = new ArrayList<>();

        genres.forEach(genre ->{
            if(genre.equals("voteCount")){
                List<Movie> movies = movieRepository.findTop10ByOrderByVoteCountDesc();
                homePageMovieList.add(movieMapper.toListMovieResponse(movies));
            }else {
                List<Movie> movies = movieRepository.findTop10ByGenres_NameOrderByIdDesc(genre);
                homePageMovieList.add(movieMapper.toListMovieResponse(movies));
            }
        });
            return homePageMovieList;
    }

    public List<MovieResponse> searchMovies(String keyword){
        List<Movie> movies = movieRepository.searchMovies(keyword);
        return movieMapper.toListMovieResponse(movies);
    }

    public Page<MovieResponse> getMovieForPage(int page, int size){
        Pageable pageable = PageRequest.of(page,size);
        Page<Movie> pageMovie = movieRepository.findAll(pageable);
        return pageMovie.map(movieMapper::toMovieResponse);
    }

    public List<MovieResponse> getFavoriteMovie(String userId){
        List<Movie> favoriteMovies = movieRepository.findFavoriteMoviesByUserId(userId);
        return movieMapper.toListMovieResponse(favoriteMovies);
    }
    public boolean isLiked(String userId,Long movieId){

        return userRepository.existsByIdAndFavoriteMovies_Id(userId,movieId);
    }

}
