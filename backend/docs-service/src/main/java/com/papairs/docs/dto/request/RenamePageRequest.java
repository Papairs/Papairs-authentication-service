package com.papairs.docs.dto.request;

import jakarta.validation.constraints.NotBlank;

public class RenamePageRequest {
    @NotBlank(message = "New title is required")
    private String newTitle;

    public RenamePageRequest() {
    }

    public RenamePageRequest(String newTitle) {
        this.newTitle = newTitle;
    }

    public String getNewTitle() {
        return newTitle;
    }

    public void setNewTitle(String newTitle) {
        this.newTitle = newTitle;
    }
}
