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

import com.longtapcode.identity_service.dto.request.CastRequest;
import com.longtapcode.identity_service.dto.response.CastResponse;
import com.longtapcode.identity_service.entity.Cast;
import com.longtapcode.identity_service.exception.AppException;
import com.longtapcode.identity_service.exception.ErrorCode;
import com.longtapcode.identity_service.mapper.CastMapper;
import com.longtapcode.identity_service.repository.CastRepository;

@ExtendWith(MockitoExtension.class)
class CastServiceTest {

    @Mock
    private CastRepository castRepository;

    @Mock
    private CastMapper castMapper;

    @InjectMocks
    private CastService castService;

    private Cast cast;
    private CastResponse castResponse;

    @BeforeEach
    void setUp() {
        cast = Cast.builder().id(1L).name("Tom Hanks").profilePath("/tom.jpg").build();
        castResponse = new CastResponse(1L, "Tom Hanks", "/tom.jpg");
    }

    @Nested
    @DisplayName("getCastById")
    class GetCastById {

        @Test
        @DisplayName("Success - should return cast by id")
        void getCastById_Success() {
            when(castRepository.findById(1L)).thenReturn(Optional.of(cast));
            when(castMapper.toCastResponse(cast)).thenReturn(castResponse);

            CastResponse result = castService.getCastById(1L);

            assertNotNull(result);
            assertEquals("Tom Hanks", result.getName());
        }

        @Test
        @DisplayName("Fail - should throw when cast not found")
        void getCastById_Fail_NotFound() {
            when(castRepository.findById(999L)).thenReturn(Optional.empty());

            AppException ex = assertThrows(AppException.class, () -> castService.getCastById(999L));

            assertEquals(ErrorCode.CAST_NOT_EXISTED, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("getAllCast")
    class GetAllCast {

        @Test
        @DisplayName("Success - should return all casts")
        void getAllCast_Success() {
            when(castRepository.findAll()).thenReturn(List.of(cast));
            when(castMapper.toListCastResponse(anyList())).thenReturn(List.of(castResponse));

            List<CastResponse> result = castService.getAllCast();

            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Success - should return empty list when no casts")
        void getAllCast_EmptyList() {
            when(castRepository.findAll()).thenReturn(Collections.emptyList());
            when(castMapper.toListCastResponse(anyList())).thenReturn(Collections.emptyList());

            List<CastResponse> result = castService.getAllCast();

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("getAllCasts (Paged)")
    class GetAllCastsPaged {

        @Test
        @DisplayName("Success - should return paged casts without search")
        void getAllCasts_NoSearch() {
            Page<Cast> castPage = new PageImpl<>(List.of(cast));
            when(castRepository.findAll(any(Pageable.class))).thenReturn(castPage);
            when(castMapper.toCastResponse(any())).thenReturn(castResponse);

            Page<CastResponse> result = castService.getAllCasts(0, 10, null);

            assertEquals(1, result.getTotalElements());
        }

        @Test
        @DisplayName("Success - should return paged casts with search keyword")
        void getAllCasts_WithSearch() {
            Page<Cast> castPage = new PageImpl<>(List.of(cast));
            when(castRepository.findByNameContainingIgnoreCase(eq("Tom"), any(Pageable.class)))
                    .thenReturn(castPage);
            when(castMapper.toCastResponse(any())).thenReturn(castResponse);

            Page<CastResponse> result = castService.getAllCasts(0, 10, "Tom");

            assertEquals(1, result.getTotalElements());
        }
    }

    @Nested
    @DisplayName("createCast")
    class CreateCast {

        @Test
        @DisplayName("Success - should create and return cast")
        void createCast_Success() {
            CastRequest request = new CastRequest("Robert Downey Jr.", "/rdj.jpg");

            when(castRepository.save(any(Cast.class))).thenReturn(cast);
            when(castMapper.toCastResponse(any())).thenReturn(castResponse);

            CastResponse result = castService.createCast(request);

            assertNotNull(result);
            verify(castRepository).save(any(Cast.class));
        }
    }

    @Nested
    @DisplayName("updateCast")
    class UpdateCast {

        @Test
        @DisplayName("Success - should update cast fields")
        void updateCast_Success() {
            CastRequest request = new CastRequest("Tom Hanks Updated", "/tom2.jpg");

            when(castRepository.findById(1L)).thenReturn(Optional.of(cast));
            when(castRepository.save(any())).thenReturn(cast);
            when(castMapper.toCastResponse(any())).thenReturn(castResponse);

            CastResponse result = castService.updateCast(1L, request);

            assertNotNull(result);
            assertEquals("Tom Hanks Updated", cast.getName());
            assertEquals("/tom2.jpg", cast.getProfilePath());
        }

        @Test
        @DisplayName("Fail - should throw when cast not found")
        void updateCast_Fail_NotFound() {
            CastRequest request = new CastRequest("Updated", "/path.jpg");
            when(castRepository.findById(999L)).thenReturn(Optional.empty());

            AppException ex = assertThrows(AppException.class, () -> castService.updateCast(999L, request));

            assertEquals(ErrorCode.CAST_NOT_EXISTED, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("deleteCast")
    class DeleteCast {

        @Test
        @DisplayName("Success - should delete cast")
        void deleteCast_Success() {
            when(castRepository.existsById(1L)).thenReturn(true);

            assertDoesNotThrow(() -> castService.deleteCast(1L));

            verify(castRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Fail - should throw when cast not found")
        void deleteCast_Fail_NotFound() {
            when(castRepository.existsById(999L)).thenReturn(false);

            AppException ex = assertThrows(AppException.class, () -> castService.deleteCast(999L));

            assertEquals(ErrorCode.CAST_NOT_EXISTED, ex.getErrorCode());
            verify(castRepository, never()).deleteById(anyLong());
        }
    }
}
