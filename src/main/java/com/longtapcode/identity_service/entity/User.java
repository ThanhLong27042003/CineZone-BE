package com.longtapcode.identity_service.entity;

import java.time.LocalDate;
import java.util.Set;

import jakarta.persistence.*;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    String userName;
    String password;
    String firstName;
    String lastName;
    String phoneNumber;
    String address;
    String emailAddress;
    String avatar;
    LocalDate dob;
    @Column(name = "isLock", nullable = false,columnDefinition = "TINYINT(1) DEFAULT 0"
    )
    boolean isLock = false;


    @ManyToMany
    Set<Role> roles;

    @ManyToMany
    @JoinTable(
            name="favoritemovies",
            joinColumns = @JoinColumn(name = "id"),
            inverseJoinColumns = @JoinColumn(name = "movieid")
    )
    @OnDelete(action= OnDeleteAction.CASCADE)
    Set<Movie> favoriteMovies;

    public void toggleLock(){
        this.isLock = !this.isLock();
    }
}
