package com.longtapcode.identity_service.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.longtapcode.identity_service.dto.request.CreationUserRequest;
import com.longtapcode.identity_service.dto.request.UpdateUserRequest;
import com.longtapcode.identity_service.dto.request.admin.AdminUpdateUserRequest;
import com.longtapcode.identity_service.dto.response.UserResponse;
import com.longtapcode.identity_service.entity.Movie;
import com.longtapcode.identity_service.entity.Role;
import com.longtapcode.identity_service.entity.User;
import com.longtapcode.identity_service.exception.AppException;
import com.longtapcode.identity_service.exception.ErrorCode;
import com.longtapcode.identity_service.mapper.UserMapper;
import com.longtapcode.identity_service.repository.MovieRepository;
import com.longtapcode.identity_service.repository.RoleRepository;
import com.longtapcode.identity_service.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private UserService userService;

    private User user;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId("user-123");
        user.setUserName("testUser");
        user.setPassword("encodedPassword");
        user.setFavoriteMovies(new HashSet<>());
        user.setRoles(new HashSet<>());

        userResponse = new UserResponse();
        userResponse.setId("user-123");
        userResponse.setUserName("testUser");
    }

    // ==================== createUser ====================
    @Nested
    @DisplayName("createUser")
    class CreateUser {

        @Test
        @DisplayName("Success - should create user and return response")
        void createUser_Success() {
            CreationUserRequest request = new CreationUserRequest();
            request.setUserName("newUser");
            request.setPassword("password123");

            when(userRepository.existsByUserName("newUser")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
            when(userMapper.toUser(any())).thenReturn(user);
            when(userRepository.save(any())).thenReturn(user);
            when(userMapper.toUserResponse(any())).thenReturn(userResponse);

            UserResponse result = userService.createUser(request);

            assertNotNull(result);
            assertEquals("user-123", result.getId());
            verify(passwordEncoder).encode("password123");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Fail - should throw when username already exists")
        void createUser_Fail_UserExisted() {
            CreationUserRequest request = new CreationUserRequest();
            request.setUserName("existingUser");
            request.setPassword("password123");

            when(userRepository.existsByUserName("existingUser")).thenReturn(true);

            AppException ex = assertThrows(AppException.class, () -> userService.createUser(request));

            assertEquals(ErrorCode.USER_EXISTED, ex.getErrorCode());
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Success - should assign USER_ROLE by default")
        void createUser_AssignsDefaultRole() {
            CreationUserRequest request = new CreationUserRequest();
            request.setUserName("newUser");
            request.setPassword("password123");

            when(userRepository.existsByUserName(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("encoded");
            when(userMapper.toUser(any())).thenReturn(user);
            when(userRepository.save(any())).thenReturn(user);
            when(userMapper.toUserResponse(any())).thenReturn(userResponse);

            userService.createUser(request);

            assertFalse(user.getRoles().isEmpty());
            assertTrue(user.getRoles().stream().anyMatch(r -> "USER".equals(r.getName())));
        }
    }

    // ==================== updateUserService ====================
    @Nested
    @DisplayName("updateUserService")
    class UpdateUserService {

        @Test
        @DisplayName("Success - should update user fields")
        void updateUser_Success() {
            UpdateUserRequest request = new UpdateUserRequest(
                    "updatedUser", "John", "Doe", "0123456789", "123 Street", "john@gmail.com", "avatar.png", null);

            when(userRepository.findById("user-123")).thenReturn(Optional.of(user));

            assertDoesNotThrow(() -> userService.updateUserService("user-123", request));

            verify(userMapper).updateUser(user, request);
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("Fail - should throw when user not found")
        void updateUser_Fail_UserNotFound() {
            UpdateUserRequest request = new UpdateUserRequest(
                    "updatedUser", "John", "Doe", "0123456789", "123 Street", "john@gmail.com", "avatar.png", null);

            when(userRepository.findById("invalid-id")).thenReturn(Optional.empty());

            AppException ex =
                    assertThrows(AppException.class, () -> userService.updateUserService("invalid-id", request));

            assertEquals(ErrorCode.USER_NOT_EXISTED, ex.getErrorCode());
            verify(userRepository, never()).save(any());
        }
    }

    // ==================== addFavoriteMovie ====================
    @Nested
    @DisplayName("addFavoriteMovie")
    class AddFavoriteMovie {

        @Test
        @DisplayName("Success - should add movie to favorites")
        void addFavoriteMovie_Success() {
            Movie movie = new Movie();
            movie.setId(1L);
            movie.setTitle("Avengers");

            when(userRepository.findById("user-123")).thenReturn(Optional.of(user));
            when(movieRepository.findById(1L)).thenReturn(Optional.of(movie));
            when(userRepository.save(any())).thenReturn(user);

            assertDoesNotThrow(() -> userService.addFavoriteMovie("user-123", 1L));

            assertTrue(user.getFavoriteMovies().contains(movie));
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("Fail - should throw when user not found")
        void addFavoriteMovie_Fail_UserNotFound() {
            when(userRepository.findById("invalid-id")).thenReturn(Optional.empty());

            AppException ex = assertThrows(AppException.class, () -> userService.addFavoriteMovie("invalid-id", 1L));

            assertEquals(ErrorCode.USER_NOT_EXISTED, ex.getErrorCode());
            verify(movieRepository, never()).findById(anyLong());
        }

        @Test
        @DisplayName("Fail - should throw when movie not found")
        void addFavoriteMovie_Fail_MovieNotFound() {
            when(userRepository.findById("user-123")).thenReturn(Optional.of(user));
            when(movieRepository.findById(999L)).thenReturn(Optional.empty());

            AppException ex = assertThrows(AppException.class, () -> userService.addFavoriteMovie("user-123", 999L));

            assertEquals(ErrorCode.MOVIE_NOT_EXISTED, ex.getErrorCode());
            verify(userRepository, never()).save(any());
        }
    }

    // ==================== removeFavoriteMovie ====================
    @Nested
    @DisplayName("removeFavoriteMovie")
    class RemoveFavoriteMovie {

        @Test
        @DisplayName("Success - should remove movie from favorites")
        void removeFavoriteMovie_Success() {
            Movie movie = new Movie();
            movie.setId(1L);
            user.getFavoriteMovies().add(movie);

            when(userRepository.findById("user-123")).thenReturn(Optional.of(user));

            assertDoesNotThrow(() -> userService.removeFavoriteMovie("user-123", 1L));

            assertFalse(
                    user.getFavoriteMovies().stream().anyMatch(m -> m.getId().equals(1L)));
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("Fail - should throw when user not found")
        void removeFavoriteMovie_Fail_UserNotFound() {
            when(userRepository.findById("invalid-id")).thenReturn(Optional.empty());

            AppException ex = assertThrows(AppException.class, () -> userService.removeFavoriteMovie("invalid-id", 1L));

            assertEquals(ErrorCode.USER_NOT_EXISTED, ex.getErrorCode());
        }
    }

    // ==================== getAllUsersForAdmin ====================
    @Nested
    @DisplayName("getAllUsersForAdmin")
    class GetAllUsersForAdmin {

        @Test
        @DisplayName("Success - should return paginated users")
        void getAllUsersForAdmin_Success() {
            Page<User> userPage = new PageImpl<>(List.of(user), PageRequest.of(0, 10), 1);
            when(userRepository.findAll(any(PageRequest.class))).thenReturn(userPage);
            when(userMapper.toUserResponse(any())).thenReturn(userResponse);

            Page<UserResponse> result = userService.getAllUsersForAdmin(0, 10);

            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
        }
    }

    // ==================== getUserById ====================
    @Nested
    @DisplayName("getUserById")
    class GetUserById {

        @Test
        @DisplayName("Success - should return user by id")
        void getUserById_Success() {
            when(userRepository.findById("user-123")).thenReturn(Optional.of(user));
            when(userMapper.toUserResponse(user)).thenReturn(userResponse);

            UserResponse result = userService.getUserById("user-123");

            assertNotNull(result);
            assertEquals("user-123", result.getId());
        }

        @Test
        @DisplayName("Fail - should throw when user not found")
        void getUserById_Fail_NotFound() {
            when(userRepository.findById("invalid")).thenReturn(Optional.empty());

            AppException ex = assertThrows(AppException.class, () -> userService.getUserById("invalid"));

            assertEquals(ErrorCode.USER_NOT_EXISTED, ex.getErrorCode());
        }
    }

    // ==================== lockUser ====================
    @Nested
    @DisplayName("lockUser")
    class LockUser {

        @Test
        @DisplayName("Success - should lock an unlocked user")
        void lockUser_Success_Lock() {
            user.setLock(false);
            when(userRepository.findById("user-123")).thenReturn(Optional.of(user));

            String result = userService.lockUser("user-123");

            assertEquals("This account is locked", result);
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("Success - should unlock a locked user")
        void lockUser_Success_Unlock() {
            user.setLock(true);
            when(userRepository.findById("user-123")).thenReturn(Optional.of(user));

            String result = userService.lockUser("user-123");

            assertEquals("This account is unlocked", result);
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("Fail - should throw when user not found")
        void lockUser_Fail_NotFound() {
            when(userRepository.findById("invalid")).thenReturn(Optional.empty());

            AppException ex = assertThrows(AppException.class, () -> userService.lockUser("invalid"));

            assertEquals(ErrorCode.USER_NOT_EXISTED, ex.getErrorCode());
        }
    }

    // ==================== updateUser (Admin) ====================
    @Nested
    @DisplayName("updateUser (Admin)")
    class AdminUpdateUser {

        @Test
        @DisplayName("Success - should update user with roles")
        void updateUser_Admin_Success() {
            AdminUpdateUserRequest request = new AdminUpdateUserRequest(
                    "updatedUser",
                    "John",
                    "Doe",
                    "0123456789",
                    "123 Street",
                    "john@gmail.com",
                    "avatar.png",
                    null,
                    Set.of("ADMIN"));

            Role adminRole = new Role();
            adminRole.setName("ADMIN");

            when(userRepository.findById("user-123")).thenReturn(Optional.of(user));
            when(roleRepository.findAllById(anyIterable())).thenReturn(List.of(adminRole));
            when(userRepository.save(any())).thenReturn(user);
            when(userMapper.toUserResponse(any())).thenReturn(userResponse);

            UserResponse result = userService.updateUser("user-123", request);

            assertNotNull(result);
            assertTrue(user.getRoles().contains(adminRole));
        }

        @Test
        @DisplayName("Fail - should throw when user not found")
        void updateUser_Admin_Fail_NotFound() {
            AdminUpdateUserRequest request = new AdminUpdateUserRequest(
                    "invalid",
                    "John",
                    "Doe",
                    "0123456789",
                    "123 Street",
                    "john@gmail.com",
                    "avatar.png",
                    null,
                    Set.of("ADMIN"));
            when(userRepository.findById("invalid")).thenReturn(Optional.empty());

            AppException ex = assertThrows(AppException.class, () -> userService.updateUser("invalid", request));

            assertEquals(ErrorCode.USER_NOT_EXISTED, ex.getErrorCode());
        }
    }
}
