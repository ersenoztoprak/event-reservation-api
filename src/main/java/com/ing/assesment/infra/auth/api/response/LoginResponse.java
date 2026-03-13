package com.ing.assesment.infra.auth.api.response;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType) {
}
