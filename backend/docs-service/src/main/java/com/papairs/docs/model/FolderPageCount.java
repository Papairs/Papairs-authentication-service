package com.papairs.docs.model;

public class FolderPageCount {
    private String folderId;
    private Long count;

    public FolderPageCount(String folderId, Long count) {
        this.folderId = folderId;
        this.count = count;
    }

    public String getFolderId() {
        return folderId;
    }

    public void setFolderId(String folderId) {
        this.folderId = folderId;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }
}
