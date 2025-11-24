package com.papairs.orchestration.dto.response;

public record AuthResponse(
        boolean success,
        String message,
        String sessionToken,
        UserResponse user
) {}