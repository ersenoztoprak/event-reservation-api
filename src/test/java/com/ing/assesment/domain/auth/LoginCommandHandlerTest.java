package com.ing.assesment.domain.auth;

import com.ing.assesment.domain.auth.command.LoginCommand;
import com.ing.assesment.domain.auth.command.handler.LoginCommandHandler;
import com.ing.assesment.domain.auth.model.TokenPair;
import com.ing.assesment.domain.auth.model.User;
import com.ing.assesment.domain.auth.port.JwtTokenPort;
import com.ing.assesment.domain.auth.port.PasswordEncoderPort;
import com.ing.assesment.domain.auth.port.UserRepositoryPort;
import com.ing.assesment.domain.common.exception.InvalidCredentialsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginCommandHandlerTest {

    @Mock
    private UserRepositoryPort userRepository;

    @Mock
    private PasswordEncoderPort passwordEncoder;

    @Mock
    private JwtTokenPort jwtTokenPort;

    private LoginCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new LoginCommandHandler(userRepository, passwordEncoder, jwtTokenPort);
    }

    @Test
    void shouldLoginSuccessfully() {
        LoginCommand command = new LoginCommand("user@test.com", "Password123");

        User user = User.create("user@test.com", "hashed-password");
        user.setId(1L);
        user.setCreatedAt(Instant.now());

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password123", "hashed-password")).thenReturn(true);
        when(jwtTokenPort.generateAccessToken(user)).thenReturn("access-token");
        when(jwtTokenPort.generateRefreshToken(user)).thenReturn("refresh-token");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TokenPair result = handler.handle(command);

        assertNotNull(result);
        assertEquals("access-token", result.accessToken());
        assertEquals("refresh-token", result.refreshToken());
        assertEquals("Bearer", result.tokenType());

        verify(userRepository).findByEmail("user@test.com");
        verify(passwordEncoder).matches("Password123", "hashed-password");
        verify(jwtTokenPort).generateAccessToken(user);
        verify(jwtTokenPort).generateRefreshToken(user);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertNotNull(savedUser.getLastLoginAt());
    }

    @Test
    void shouldThrowWhenUserNotFound() {
        LoginCommand command = new LoginCommand("missing@test.com", "Password123");

        when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> handler.handle(command));

        verify(userRepository).findByEmail("missing@test.com");
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtTokenPort, never()).generateAccessToken(any());
        verify(jwtTokenPort, never()).generateRefreshToken(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenPasswordIsWrong() {
        LoginCommand command = new LoginCommand("user@test.com", "wrong-password");

        User user = User.create("user@test.com", "hashed-password");
        user.setId(1L);

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "hashed-password")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> handler.handle(command));

        verify(userRepository).findByEmail("user@test.com");
        verify(passwordEncoder).matches("wrong-password", "hashed-password");
        verify(jwtTokenPort, never()).generateAccessToken(any());
        verify(jwtTokenPort, never()).generateRefreshToken(any());
        verify(userRepository, never()).save(any());
    }
}
