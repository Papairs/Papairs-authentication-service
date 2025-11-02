package com.papairs.docs.dto.request;

import jakarta.validation.constraints.NotBlank;

public class MovePageRequest {
    @NotBlank(message = "New parent ID is required")
    private String newParentId;

    public MovePageRequest() {
    }

    public String getNewParentId() {
        return newParentId;
    }

    public void setNewParentId(String newParentId) {
        this.newParentId = newParentId;
    }
}
