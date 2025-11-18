package com.papairs.orchestration.dto.response;

import java.time.LocalDateTime;

public record UserResponse(
        String id,
        String email,
        boolean emailVerified,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime lastLoginAt
) {}