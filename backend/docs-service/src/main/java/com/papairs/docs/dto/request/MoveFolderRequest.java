package com.papairs.docs.dto.request;

import com.papairs.docs.annotation.Sanitize;
import jakarta.annotation.Nullable;

public class MoveFolderRequest {
    @Nullable
    @Sanitize(trim = true, blankToNull = true)
    private String parentFolderId;

    public MoveFolderRequest() {
    }

    public String getParentFolderId() {
        return parentFolderId;
    }

    public void setParentFolderId(String parentFolderId) {
        this.parentFolderId = parentFolderId;
    }
}
