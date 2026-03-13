package com.ing.assesment.infra.auth.security.jwt;

import com.ing.assesment.domain.auth.model.User;
import com.ing.assesment.domain.auth.port.JwtTokenPort;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class JwtTokenAdapter implements JwtTokenPort {

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    public JwtTokenAdapter(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(jwtProperties.accessTokenExpirationSeconds());

        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .issuer(jwtProperties.issuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .claim("email", user.getEmail())
                .claim("roles", user.getRoles().stream().map(Enum::name).collect(Collectors.toSet()))
                .claim("type", "access")
                .signWith(secretKey)
                .compact();
    }

    @Override
    public String generateRefreshToken(User user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(jwtProperties.refreshTokenExpirationSeconds());

        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .issuer(jwtProperties.issuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .claim("type", "refresh")
                .signWith(secretKey)
                .compact();
    }

    @Override
    public boolean isRefreshTokenValid(String refreshToken) {
        try {
            Claims claims = parseClaims(refreshToken);
            Object type = claims.get("type");
            return "refresh".equals(type);
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Invalid refresh token provided", e);
            return false;
        }
    }

    @Override
    public Long extractUserId(String token) {
        Claims claims = parseClaims(token);
        return Long.valueOf(claims.getSubject());
    }

    @Override
    public String extractEmail(String token) {
        return parseClaims(token).get("email", String.class);
    }

    @Override
    public Set<String> extractRoles(String token) {
        Object roles = parseClaims(token).get("roles");
        if (roles instanceof Set<?> set) {
            return set.stream().map(String::valueOf).collect(Collectors.toSet());
        }
        if (roles instanceof java.util.List<?> list) {
            return list.stream().map(String::valueOf).collect(Collectors.toSet());
        }
        return Set.of();
    }

    @Override
    public boolean isAccessTokenValid(String accessToken) {
        try {
            Claims claims = parseClaims(accessToken);
            return "access".equals(claims.get("type"));
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Invalid access token provided", e);
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
