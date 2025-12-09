package com.longtapcode.identity_service.dto.request.admin;

import com.longtapcode.identity_service.entity.Cast;
import com.longtapcode.identity_service.entity.Genre;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MovieRequest {
    String title;
    String overview;
    LocalDate releaseDate;
    Integer runtime;
    String posterPath;
    String backdropPath;
    BigDecimal voteAverage;
    Integer voteCount;
    Set<Long> castIds;
    Set<Long> genreIds;
}
