package com.papairs.docs.dto.request;

public class MoveFolderRequest {
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
