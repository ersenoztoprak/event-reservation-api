package com.ing.assesment.domain.auth;

import com.ing.assesment.domain.auth.command.RefreshTokenCommand;
import com.ing.assesment.domain.auth.command.handler.RefreshTokenCommandHandler;
import com.ing.assesment.domain.auth.model.TokenPair;
import com.ing.assesment.domain.auth.model.User;
import com.ing.assesment.domain.auth.port.JwtTokenPort;
import com.ing.assesment.domain.auth.port.UserRepositoryPort;
import com.ing.assesment.domain.common.exception.InvalidRefreshTokenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenCommandHandlerTest {

    @Mock
    private JwtTokenPort jwtTokenPort;

    @Mock
    private UserRepositoryPort userRepository;

    private RefreshTokenCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new RefreshTokenCommandHandler(jwtTokenPort, userRepository);
    }

    @Test
    void shouldRefreshAccessTokenSuccessfully() {
        String refreshToken = "valid-refresh-token";
        RefreshTokenCommand command = new RefreshTokenCommand(refreshToken);

        User user = User.create("user@test.com", "hashed-password");
        user.setId(1L);
        user.setCreatedAt(Instant.now());

        when(jwtTokenPort.isRefreshTokenValid(refreshToken)).thenReturn(true);
        when(jwtTokenPort.extractUserId(refreshToken)).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(jwtTokenPort.generateAccessToken(user)).thenReturn("new-access-token");

        TokenPair result = handler.handle(command);

        assertNotNull(result);
        assertEquals("new-access-token", result.accessToken());
        assertEquals(refreshToken, result.refreshToken());
        assertEquals("Bearer", result.tokenType());

        verify(jwtTokenPort).isRefreshTokenValid(refreshToken);
        verify(jwtTokenPort).extractUserId(refreshToken);
        verify(userRepository).findById(1L);
        verify(jwtTokenPort).generateAccessToken(user);
        verifyNoMoreInteractions(userRepository, jwtTokenPort);
    }

    @Test
    void shouldThrowWhenRefreshTokenIsInvalid() {
        String refreshToken = "invalid-refresh-token";
        RefreshTokenCommand command = new RefreshTokenCommand(refreshToken);

        when(jwtTokenPort.isRefreshTokenValid(refreshToken)).thenReturn(false);

        assertThrows(InvalidRefreshTokenException.class, () -> handler.handle(command));

        verify(jwtTokenPort).isRefreshTokenValid(refreshToken);
        verify(jwtTokenPort, never()).extractUserId(anyString());
        verify(userRepository, never()).findById(anyLong());
        verify(jwtTokenPort, never()).generateAccessToken(any());
    }

    @Test
    void shouldThrowWhenUserNotFound() {
        String refreshToken = "valid-refresh-token";
        RefreshTokenCommand command = new RefreshTokenCommand(refreshToken);

        when(jwtTokenPort.isRefreshTokenValid(refreshToken)).thenReturn(true);
        when(jwtTokenPort.extractUserId(refreshToken)).thenReturn(99L);
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(InvalidRefreshTokenException.class, () -> handler.handle(command));

        verify(jwtTokenPort).isRefreshTokenValid(refreshToken);
        verify(jwtTokenPort).extractUserId(refreshToken);
        verify(userRepository).findById(99L);
        verify(jwtTokenPort, never()).generateAccessToken(any());
    }
}
