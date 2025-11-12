package com.papairs.docs.dto.request;

import com.papairs.docs.annotation.Sanitize;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreatePageRequest {
    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title cannot exceed 255 characters")
    @Sanitize(trim = true)
    private String title;
    @Nullable
    @Sanitize(trim = true, blankToNull = true)
    private String folderId;

    public CreatePageRequest() {
    }

    public String getFolderId() {
        return folderId;
    }

    public void setFolderId(String folderId) {
        this.folderId = folderId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
