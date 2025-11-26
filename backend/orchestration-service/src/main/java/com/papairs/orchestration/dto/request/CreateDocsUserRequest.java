package com.papairs.orchestration.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateDocsUserRequest(
        @NotBlank(message = "User ID is required")
        String userId
) {}