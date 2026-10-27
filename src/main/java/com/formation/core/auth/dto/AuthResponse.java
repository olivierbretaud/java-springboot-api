package com.formation.core.auth.dto;

import lombok.Getter;

@Getter
public class AuthResponse {
    private final String token;
    private final String refreshToken;
    private final long expiresIn;

    public AuthResponse(String token, String refreshToken, long expiresIn) {
        this.token = token;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
    }
}
