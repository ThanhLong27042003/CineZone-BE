package com.longtapcode.identity_service.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import com.longtapcode.identity_service.dto.request.admin.MovieRequest;
import com.longtapcode.identity_service.dto.request.admin.UpdateMovieRequest;
import com.longtapcode.identity_service.dto.response.MovieResponse;
import com.longtapcode.identity_service.entity.Cast;
import com.longtapcode.identity_service.entity.Genre;
import com.longtapcode.identity_service.entity.Movie;
import com.longtapcode.identity_service.exception.AppException;
import com.longtapcode.identity_service.exception.ErrorCode;
import com.longtapcode.identity_service.mapper.MovieMapper;
import com.longtapcode.identity_service.repository.CastRepository;
import com.longtapcode.identity_service.repository.GenreRepository;
import com.longtapcode.identity_service.repository.MovieRepository;
import com.longtapcode.identity_service.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class MovieServiceTest {

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private MovieMapper movieMapper;

    @Mock
    private CastRepository castRepository;

    @Mock
    private GenreRepository genreRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private MovieService movieService;

    private Movie movie;
    private MovieResponse movieResponse;

    @BeforeEach
    void setUp() {
        movie = Movie.builder()
                .id(1L)
                .title("Avengers: Endgame")
                .overview("Epic movie")
                .runtime(180)
                .voteCount(1000)
                .genres(new HashSet<>())
                .casts(new HashSet<>())
                .build();

        movieResponse =
                MovieResponse.builder().id(1L).title("Avengers: Endgame").build();
    }

    // ==================== getMovieById ====================
    @Nested
    @DisplayName("getMovieById")
    class GetMovieById {

        @Test
        @DisplayName("Success")
        void getMovieById_Success() {
            when(movieRepository.findById(1L)).thenReturn(Optional.of(movie));
            when(movieMapper.toMovieResponse(movie)).thenReturn(movieResponse);

            MovieResponse result = movieService.getMovieById(1L);

            assertEquals("Avengers: Endgame", result.getTitle());
        }

        @Test
        @DisplayName("Fail - movie not found")
        void getMovieById_Fail() {
            when(movieRepository.findById(999L)).thenReturn(Optional.empty());

            AppException ex = assertThrows(AppException.class, () -> movieService.getMovieById(999L));

            assertEquals(ErrorCode.MOVIE_NOT_EXISTED, ex.getErrorCode());
        }
    }

    // ==================== getAllMovie ====================
    @Nested
    @DisplayName("getAllMovie")
    class GetAllMovie {

        @Test
        @DisplayName("Success - returns all movies")
        void getAllMovie_Success() {
            when(movieRepository.findAll()).thenReturn(List.of(movie));
            when(movieMapper.toListMovieResponse(anyList())).thenReturn(List.of(movieResponse));

            List<MovieResponse> result = movieService.getAllMovie();

            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Success - returns empty list")
        void getAllMovie_Empty() {
            when(movieRepository.findAll()).thenReturn(Collections.emptyList());
            when(movieMapper.toListMovieResponse(anyList())).thenReturn(Collections.emptyList());

            List<MovieResponse> result = movieService.getAllMovie();

            assertTrue(result.isEmpty());
        }
    }

    // ==================== getTopMovieForHomePage ====================
    @Nested
    @DisplayName("getTopMovieForHomePage")
    class GetTopMovieForHomePage {

        @Test
        @DisplayName("Success - voteCount genre returns top voted movies")
        void getTopMovieForHomePage_VoteCount() {
            when(movieRepository.findTop10ByOrderByVoteCountDesc()).thenReturn(List.of(movie));
            when(movieMapper.toListMovieResponse(anyList())).thenReturn(List.of(movieResponse));

            List<List<MovieResponse>> result = movieService.getTopMovieForHomePage(List.of("voteCount"));

            assertEquals(1, result.size());
            assertEquals(1, result.get(0).size());
        }

        @Test
        @DisplayName("Success - specific genre returns filtered movies")
        void getTopMovieForHomePage_SpecificGenre() {
            when(movieRepository.findTop10ByGenres_NameOrderByIdDesc("Action")).thenReturn(List.of(movie));
            when(movieMapper.toListMovieResponse(anyList())).thenReturn(List.of(movieResponse));

            List<List<MovieResponse>> result = movieService.getTopMovieForHomePage(List.of("Action"));

            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Success - multiple genres return multiple lists")
        void getTopMovieForHomePage_MultipleGenres() {
            when(movieRepository.findTop10ByOrderByVoteCountDesc()).thenReturn(List.of(movie));
            when(movieRepository.findTop10ByGenres_NameOrderByIdDesc("Action")).thenReturn(List.of(movie));
            when(movieMapper.toListMovieResponse(anyList())).thenReturn(List.of(movieResponse));

            List<List<MovieResponse>> result = movieService.getTopMovieForHomePage(List.of("voteCount", "Action"));

            assertEquals(2, result.size());
        }
    }

    // ==================== searchMovies ====================
    @Nested
    @DisplayName("searchMovies")
    class SearchMovies {

        @Test
        @DisplayName("Success - returns matching movies")
        void searchMovies_Success() {
            when(movieRepository.searchMovies("Avengers")).thenReturn(List.of(movie));
            when(movieMapper.toListMovieResponse(anyList())).thenReturn(List.of(movieResponse));

            List<MovieResponse> result = movieService.searchMovies("Avengers");

            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Success - returns empty for no matches")
        void searchMovies_NoResult() {
            when(movieRepository.searchMovies("NonExistent")).thenReturn(Collections.emptyList());
            when(movieMapper.toListMovieResponse(anyList())).thenReturn(Collections.emptyList());

            List<MovieResponse> result = movieService.searchMovies("NonExistent");

            assertTrue(result.isEmpty());
        }
    }

    // ==================== isLiked ====================
    @Nested
    @DisplayName("isLiked")
    class IsLiked {

        @Test
        @DisplayName("Returns true when user liked movie")
        void isLiked_True() {
            when(userRepository.existsByIdAndFavoriteMovies_Id("user-123", 1L)).thenReturn(true);

            assertTrue(movieService.isLiked("user-123", 1L));
        }

        @Test
        @DisplayName("Returns false when user has not liked movie")
        void isLiked_False() {
            when(userRepository.existsByIdAndFavoriteMovies_Id("user-123", 1L)).thenReturn(false);

            assertFalse(movieService.isLiked("user-123", 1L));
        }
    }

    // ==================== createMovie (Admin) ====================
    @Nested
    @DisplayName("createMovie (Admin)")
    class CreateMovie {

        @Test
        @DisplayName("Success - with genres and casts")
        void createMovie_WithGenresAndCasts() {
            MovieRequest request = new MovieRequest();
            request.setGenreIds(Set.of(1L));
            request.setCastIds(Set.of(1L));

            Genre genre = Genre.builder().id(1L).name("Action").build();
            Cast cast = Cast.builder().id(1L).name("Tom").build();

            when(movieMapper.toMovie(any())).thenReturn(movie);
            when(genreRepository.findAllById(anyIterable())).thenReturn(List.of(genre));
            when(castRepository.findAllById(anyIterable())).thenReturn(List.of(cast));
            when(movieRepository.save(any())).thenReturn(movie);
            when(movieMapper.toMovieResponse(any())).thenReturn(movieResponse);

            MovieResponse result = movieService.createMovie(request);

            assertNotNull(result);
            assertTrue(movie.getGenres().contains(genre));
            assertTrue(movie.getCasts().contains(cast));
        }

        @Test
        @DisplayName("Success - without genres and casts (null)")
        void createMovie_WithoutGenresAndCasts() {
            MovieRequest request = new MovieRequest();

            when(movieMapper.toMovie(any())).thenReturn(movie);
            when(movieRepository.save(any())).thenReturn(movie);
            when(movieMapper.toMovieResponse(any())).thenReturn(movieResponse);

            MovieResponse result = movieService.createMovie(request);

            assertNotNull(result);
            verify(genreRepository, never()).findAllById(anyIterable());
            verify(castRepository, never()).findAllById(anyIterable());
        }
    }

    // ==================== updateMovie (Admin) ====================
    @Nested
    @DisplayName("updateMovie (Admin)")
    class UpdateMovie {

        @Test
        @DisplayName("Success")
        void updateMovie_Success() {
            UpdateMovieRequest request = new UpdateMovieRequest();
            request.setGenreIds(Set.of(1L));

            Genre genre = Genre.builder().id(1L).name("Comedy").build();

            when(movieRepository.findById(1L)).thenReturn(Optional.of(movie));
            when(genreRepository.findAllById(anyIterable())).thenReturn(List.of(genre));
            when(movieRepository.save(any())).thenReturn(movie);
            when(movieMapper.toMovieResponse(any())).thenReturn(movieResponse);

            MovieResponse result = movieService.updateMovie(1L, request);

            assertNotNull(result);
            verify(movieMapper).updateMovie(movie, request);
        }

        @Test
        @DisplayName("Fail - movie not found")
        void updateMovie_Fail() {
            UpdateMovieRequest request = new UpdateMovieRequest();
            when(movieRepository.findById(999L)).thenReturn(Optional.empty());

            AppException ex = assertThrows(AppException.class, () -> movieService.updateMovie(999L, request));

            assertEquals(ErrorCode.MOVIE_NOT_EXISTED, ex.getErrorCode());
        }
    }

    // ==================== deleteMovieById (Admin) ====================
    @Nested
    @DisplayName("deleteMovieById (Admin)")
    class DeleteMovie {

        @Test
        @DisplayName("Success")
        void deleteMovie_Success() {
            when(movieRepository.existsById(1L)).thenReturn(true);

            assertDoesNotThrow(() -> movieService.deleteMovieById(1L));

            verify(movieRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Fail - movie not found")
        void deleteMovie_Fail() {
            when(movieRepository.existsById(999L)).thenReturn(false);

            AppException ex = assertThrows(AppException.class, () -> movieService.deleteMovieById(999L));

            assertEquals(ErrorCode.MOVIE_NOT_EXISTED, ex.getErrorCode());
        }
    }
}
