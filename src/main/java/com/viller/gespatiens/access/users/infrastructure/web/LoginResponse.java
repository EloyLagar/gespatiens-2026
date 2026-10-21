package com.viller.gespatiens.access.users.infrastructure.web;

import com.viller.gespatiens.access.users.application.LoginUseCase;

import java.time.Instant;

// El frontend guarda el token y lo envía en cada petición: "Authorization: Bearer <accessToken>"
public record LoginResponse(String accessToken, String tokenType, Instant expiresAt, UserResponse user) {

    public static LoginResponse from(LoginUseCase.Session session) {
        return new LoginResponse(session.token().value(), "Bearer", session.token().expiresAt(),
                UserResponse.from(session.user()));
    }
}
