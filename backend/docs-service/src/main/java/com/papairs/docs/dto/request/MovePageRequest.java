package com.papairs.docs.dto.request;

import com.papairs.docs.annotation.Sanitize;
import jakarta.annotation.Nullable;

public class MovePageRequest {
    @Nullable
    @Sanitize(trim = true, blankToNull = true)
    private String folderId;

    public MovePageRequest() {
    }

    public String getFolderId() {
        return folderId;
    }

    public void setFolderId(String folderId) {
        this.folderId = folderId;
    }
}
