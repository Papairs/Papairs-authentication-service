package com.papairs.docs.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateFolderRequest {
    @NotBlank(message = "Folder name is required")
    @Size(max = 255, message = "Folder name cannot exceed 255 characters")
    private String name;
    private String parentFolderId;

    public CreateFolderRequest() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getParentFolderId() {
        return parentFolderId;
    }

    public void setParentFolderId(String parentFolderId) {
        this.parentFolderId = parentFolderId;
    }
}
