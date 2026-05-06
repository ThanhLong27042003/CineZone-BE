package com.longtapcode.identity_service.mapper;

import java.util.List;

import org.mapstruct.Mapper;

import com.longtapcode.identity_service.dto.response.GenreResponse;
import com.longtapcode.identity_service.entity.Genre;

@Mapper(componentModel = "spring")
public interface GenreMapper {
    List<GenreResponse> toListGenreResponse(List<Genre> genre);

    GenreResponse toGenreResponse(Genre genre);
}
