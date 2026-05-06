package com.longtapcode.identity_service.dto.request.admin;

import java.time.LocalDate;
import java.util.Set;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateMovieRequest {
    String title;
    String overview;
    LocalDate releaseDate;
    Integer runtime;
    String posterPath;
    String backdropPath;
    Set<Long> castIds;
    Set<Long> genreIds;
}
