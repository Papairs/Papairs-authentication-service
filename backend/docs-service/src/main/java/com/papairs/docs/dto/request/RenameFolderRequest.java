package com.papairs.docs.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RenameFolderRequest {
    @NotBlank(message = "Folder name is required")
    @Size(max = 255, message = "Folder name cannot exceed 255 characters")
    private String newName;

    public RenameFolderRequest() {
    }

    public String getNewName() {
        return newName;
    }

    public void setNewName(String newName) {
        this.newName = newName;
    }
}
