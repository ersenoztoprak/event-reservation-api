package com.ing.assesment.domain.auth.port;

import com.ing.assesment.domain.auth.model.User;

import java.util.Set;

public interface JwtTokenPort {
    String generateAccessToken(User user);

    String generateRefreshToken(User user);

    boolean isRefreshTokenValid(String refreshToken);

    Long extractUserId(String token);

    String extractEmail(String token);

    Set<String> extractRoles(String token);

    boolean isAccessTokenValid(String accessToken);
}
