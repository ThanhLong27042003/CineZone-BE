package com.longtapcode.identity_service.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

@Entity
@Table(name = "movies")
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
public class Movie {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MovieID", nullable = false)
     Long id;

    @Size(max = 255)
    @NotNull
    @Column(name = "Title", nullable = false)
     String title;

    @Lob
    @Column(name = "Overview", columnDefinition = "TEXT")
     String overview;

    @Column(name = "ReleaseDate")
     LocalDate releaseDate;

    @Column(name = "Runtime")
     Integer runtime;

    @Size(max = 255)
    @Column(name = "PosterPath")
     String posterPath;

    @Size(max = 255)
    @Column(name = "BackdropPath")
     String backdropPath;

    @Column(name = "VoteAverage", precision = 3, scale = 1)
     BigDecimal voteAverage;

    @Column(name = "VoteCount")
     Integer voteCount;

    @Column(name = "trailer")
    String trailer;

    @ManyToMany
    @JoinTable(
            name="moviecasts",
            joinColumns = @JoinColumn(name = "movieid"),
            inverseJoinColumns = @JoinColumn(name = "castid")
    )
    @OnDelete(action = OnDeleteAction.CASCADE)
    Set<Cast> casts;

    @ManyToMany
    @JoinTable(
            name="moviegenres",
            joinColumns = @JoinColumn(name = "movieid"),
            inverseJoinColumns = @JoinColumn(name = "genreid")
    )
    @OnDelete(action = OnDeleteAction.CASCADE)
    Set<Genre> genres;

}