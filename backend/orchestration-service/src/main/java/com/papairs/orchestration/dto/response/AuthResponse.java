package com.papairs.orchestration.dto.response;

import java.time.LocalDateTime;

public record AuthResponse(
        String token,
        String sessionId,
        LocalDateTime expiresAt,
        UserResponse user
) {}