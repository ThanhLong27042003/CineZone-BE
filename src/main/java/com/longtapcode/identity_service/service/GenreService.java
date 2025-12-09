package com.longtapcode.identity_service.service;

import com.longtapcode.identity_service.dto.response.CastResponse;
import com.longtapcode.identity_service.dto.response.GenreResponse;
import com.longtapcode.identity_service.entity.Genre;
import com.longtapcode.identity_service.exception.AppException;
import com.longtapcode.identity_service.exception.ErrorCode;
import com.longtapcode.identity_service.mapper.GenreMapper;
import com.longtapcode.identity_service.repository.GenreRepository;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GenreService {
    GenreRepository genreRepository;
    GenreMapper genreMapper;

    public List<GenreResponse> getAllGenre(){
        return genreMapper.toListGenreResponse(genreRepository.findAll());
    }

    public GenreResponse getGenreById(Long genreId){
        Genre genre = genreRepository.findById(genreId).orElseThrow(()->new AppException(ErrorCode.GENRE_NOT_EXISTED));
        return genreMapper.toGenreResponse(genre);
    }
}
