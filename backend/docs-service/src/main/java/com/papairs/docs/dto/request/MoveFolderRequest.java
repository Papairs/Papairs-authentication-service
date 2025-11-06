package com.papairs.docs.dto.request;

public class MoveFolderRequest {
    private String newParentFolderId;

    public MoveFolderRequest() {
    }

    public String getNewParentFolderId() {
        return newParentFolderId;
    }

    public void setNewParentFolderId(String newParentFolderId) {
        this.newParentFolderId = newParentFolderId;
    }
}
