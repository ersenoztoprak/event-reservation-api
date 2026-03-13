package com.ing.assesment.infra.auth.api.response;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType) {
}
