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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.longtapcode.identity_service.dto.request.GenreRequest;
import com.longtapcode.identity_service.dto.response.GenreResponse;
import com.longtapcode.identity_service.entity.Genre;
import com.longtapcode.identity_service.exception.AppException;
import com.longtapcode.identity_service.exception.ErrorCode;
import com.longtapcode.identity_service.mapper.GenreMapper;
import com.longtapcode.identity_service.repository.GenreRepository;

@ExtendWith(MockitoExtension.class)
class GenreServiceTest {

    @Mock
    private GenreRepository genreRepository;

    @Mock
    private GenreMapper genreMapper;

    @InjectMocks
    private GenreService genreService;

    private Genre genre;
    private GenreResponse genreResponse;

    @BeforeEach
    void setUp() {
        genre = Genre.builder().id(1L).name("Action").build();
        genreResponse = new GenreResponse(1L, "Action");
    }

    @Nested
    @DisplayName("getGenreById")
    class GetGenreById {

        @Test
        @DisplayName("Success")
        void getGenreById_Success() {
            when(genreRepository.findById(1L)).thenReturn(Optional.of(genre));
            when(genreMapper.toGenreResponse(genre)).thenReturn(genreResponse);

            GenreResponse result = genreService.getGenreById(1L);

            assertEquals("Action", result.getName());
        }

        @Test
        @DisplayName("Fail - not found")
        void getGenreById_Fail() {
            when(genreRepository.findById(999L)).thenReturn(Optional.empty());

            AppException ex = assertThrows(AppException.class, () -> genreService.getGenreById(999L));

            assertEquals(ErrorCode.GENRE_NOT_EXISTED, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("getAllGenre")
    class GetAllGenre {

        @Test
        @DisplayName("Success - returns list")
        void getAllGenre_Success() {
            when(genreRepository.findAll()).thenReturn(List.of(genre));
            when(genreMapper.toListGenreResponse(anyList())).thenReturn(List.of(genreResponse));

            List<GenreResponse> result = genreService.getAllGenre();

            assertEquals(1, result.size());
        }
    }

    @Nested
    @DisplayName("getAllGenres (Paged)")
    class GetAllGenresPaged {

        @Test
        @DisplayName("Success - without search")
        void getAllGenres_NoSearch() {
            Page<Genre> page = new PageImpl<>(List.of(genre));
            when(genreRepository.findAll(any(Pageable.class))).thenReturn(page);
            when(genreMapper.toGenreResponse(any())).thenReturn(genreResponse);

            Page<GenreResponse> result = genreService.getAllGenres(0, 10, null);

            assertEquals(1, result.getTotalElements());
        }

        @Test
        @DisplayName("Success - with search keyword")
        void getAllGenres_WithSearch() {
            Page<Genre> page = new PageImpl<>(List.of(genre));
            when(genreRepository.findByNameContainingIgnoreCase(eq("Act"), any(Pageable.class)))
                    .thenReturn(page);
            when(genreMapper.toGenreResponse(any())).thenReturn(genreResponse);

            Page<GenreResponse> result = genreService.getAllGenres(0, 10, "Act");

            assertEquals(1, result.getTotalElements());
        }
    }

    @Nested
    @DisplayName("createGenre")
    class CreateGenre {

        @Test
        @DisplayName("Success")
        void createGenre_Success() {
            GenreRequest request = GenreRequest.builder().name("Horror").build();
            when(genreRepository.save(any())).thenReturn(genre);
            when(genreMapper.toGenreResponse(any())).thenReturn(genreResponse);

            GenreResponse result = genreService.createGenre(request);

            assertNotNull(result);
            verify(genreRepository).save(any(Genre.class));
        }
    }

    @Nested
    @DisplayName("updateGenre")
    class UpdateGenre {

        @Test
        @DisplayName("Success")
        void updateGenre_Success() {
            GenreRequest request = GenreRequest.builder().name("Comedy").build();
            when(genreRepository.findById(1L)).thenReturn(Optional.of(genre));
            when(genreRepository.save(any())).thenReturn(genre);
            when(genreMapper.toGenreResponse(any())).thenReturn(genreResponse);

            GenreResponse result = genreService.updateGenre(1L, request);

            assertNotNull(result);
            assertEquals("Comedy", genre.getName());
        }

        @Test
        @DisplayName("Fail - not found")
        void updateGenre_Fail() {
            GenreRequest request = GenreRequest.builder().name("Comedy").build();
            when(genreRepository.findById(999L)).thenReturn(Optional.empty());

            AppException ex = assertThrows(AppException.class, () -> genreService.updateGenre(999L, request));

            assertEquals(ErrorCode.GENRE_NOT_EXISTED, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("deleteGenre")
    class DeleteGenre {

        @Test
        @DisplayName("Success")
        void deleteGenre_Success() {
            when(genreRepository.existsById(1L)).thenReturn(true);

            assertDoesNotThrow(() -> genreService.deleteGenre(1L));

            verify(genreRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Fail - not found")
        void deleteGenre_Fail() {
            when(genreRepository.existsById(999L)).thenReturn(false);

            AppException ex = assertThrows(AppException.class, () -> genreService.deleteGenre(999L));

            assertEquals(ErrorCode.GENRE_NOT_EXISTED, ex.getErrorCode());
        }
    }
}
