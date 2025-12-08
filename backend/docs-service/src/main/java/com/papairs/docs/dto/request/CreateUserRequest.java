package com.papairs.docs.dto.request;

import jakarta.validation.constraints.NotBlank;

public class CreateUserRequest {
    @NotBlank(message = "User ID is required")
    private String userId;

    public CreateUserRequest() {
    }

    public CreateUserRequest(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
