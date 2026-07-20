package com.training.starter.dto.response;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String username,
        String role
) {}
