package com.longtapcode.identity_service.mapper;

import com.longtapcode.identity_service.dto.request.MovieRequest;
import com.longtapcode.identity_service.dto.request.UpdateMovieRequest;
import com.longtapcode.identity_service.dto.response.MovieResponse;
import com.longtapcode.identity_service.entity.Movie;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface MovieMapper {
    @Mapping(target = "genres", ignore = true)
    @Mapping(target = "casts", ignore = true)
    Movie toMovie(MovieRequest request);
    MovieResponse toMovieResponse(Movie movie);
    List<MovieResponse> toListMovieResponse(List<Movie> movie);
    void updateMovie(@MappingTarget Movie movie, UpdateMovieRequest request);
}
