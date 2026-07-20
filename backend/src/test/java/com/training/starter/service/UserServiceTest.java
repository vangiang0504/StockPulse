package com.training.starter.service;

import com.training.starter.dto.request.CreateUserRequest;
import com.training.starter.dto.request.UpdateUserRequest;
import com.training.starter.dto.response.UserResponse;
import com.training.starter.entity.User;
import com.training.starter.enums.Role;
import com.training.starter.exception.DuplicateResourceException;
import com.training.starter.exception.ResourceNotFoundException;
import com.training.starter.mapper.UserMapper;
import com.training.starter.repository.UserRepository;
import com.training.starter.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void create_validRequest_returnsUserResponse() {
        // Given
        var request = new CreateUserRequest("testuser", "test@example.com", "password123", "Test User");
        var entity = buildUser(1L, "testuser", "test@example.com");
        var response = new UserResponse(1L, "testuser", "test@example.com", "Test User", "USER", true, LocalDateTime.now());

        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(userMapper.toEntity(request)).thenReturn(entity);
        when(passwordEncoder.encode("password123")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenReturn(entity);
        when(userMapper.toResponse(entity)).thenReturn(response);

        // When
        var result = userService.create(request);

        // Then
        assertThat(result.username()).isEqualTo("testuser");
        assertThat(result.email()).isEqualTo("test@example.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void create_duplicateUsername_throwsDuplicateResourceException() {
        // Given
        var request = new CreateUserRequest("existing", "new@example.com", "password", "Test");
        when(userRepository.existsByUsername("existing")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(DuplicateResourceException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void create_duplicateEmail_throwsDuplicateResourceException() {
        // Given
        var request = new CreateUserRequest("newuser", "existing@example.com", "password", "Test");
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(DuplicateResourceException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void getById_found_returnsUserResponse() {
        // Given
        var entity = buildUser(1L, "testuser", "test@example.com");
        var response = new UserResponse(1L, "testuser", "test@example.com", "Test User", "USER", true, LocalDateTime.now());

        when(userRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(userMapper.toResponse(entity)).thenReturn(response);

        // When
        var result = userService.getById(1L);

        // Then
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.username()).isEqualTo("testuser");
    }

    @Test
    void getById_notFound_throwsResourceNotFoundException() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.getById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_validRequest_updatesAndReturns() {
        // Given
        var entity = buildUser(1L, "testuser", "test@example.com");
        var request = new UpdateUserRequest("new@example.com", "New Name", null);
        var response = new UserResponse(1L, "testuser", "new@example.com", "New Name", "USER", true, LocalDateTime.now());

        when(userRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.save(entity)).thenReturn(entity);
        when(userMapper.toResponse(entity)).thenReturn(response);

        // When
        var result = userService.update(1L, request);

        // Then
        assertThat(result.email()).isEqualTo("new@example.com");
        verify(userMapper).updateEntity(entity, request);
    }

    @Test
    void delete_existingUser_deletesSuccessfully() {
        // Given
        var entity = buildUser(1L, "testuser", "test@example.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(entity));

        // When
        userService.delete(1L);

        // Then
        verify(userRepository).delete(entity);
    }

    private User buildUser(Long id, String username, String email) {
        User user = User.builder()
                .username(username)
                .email(email)
                .password("encoded")
                .fullName("Test User")
                .role(Role.USER)
                .active(true)
                .build();
        user.setId(id);
        return user;
    }
}
