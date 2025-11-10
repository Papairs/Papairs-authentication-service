package com.papairs.docs.dto.request;

import com.papairs.docs.annotation.Sanitize;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RenamePageRequest {
    @NotBlank(message = "New title is required")
    @Size(max = 255, message = "Title name cannot exceed 255 characters")
    @Sanitize(trim = true, blankToNull = true)
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
