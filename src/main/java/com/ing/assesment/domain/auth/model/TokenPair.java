package com.ing.assesment.domain.auth.model;

public record TokenPair(
        String accessToken,
        String refreshToken,
        String tokenType) {

    public static TokenPair bearer(String accessToken, String refreshToken) {
        return new TokenPair(accessToken, refreshToken, "Bearer");
    }
}
