package com.longtapcode.identity_service.mapper;

import com.longtapcode.identity_service.dto.response.GenreResponse;
import com.longtapcode.identity_service.entity.Genre;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface GenreMapper {
    List<GenreResponse> toListGenreResponse(List<Genre> genre);
    GenreResponse toGenreResponse(Genre genre);
}
