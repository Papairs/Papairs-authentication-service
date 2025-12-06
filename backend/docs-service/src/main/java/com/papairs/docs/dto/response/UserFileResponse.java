package com.papairs.docs.dto.response;

public class UserFileResponse {
    private String fileId;
    private String filename;
    private Long fileSize;
    private String mimeType;
    private String createdAt;
    private String b2FileId;
    private String b2FilePath;

    public UserFileResponse() {}

    public UserFileResponse(String fileId, String filename, Long fileSize, String mimeType, 
                           String createdAt, String b2FileId, String b2FilePath) {
        this.fileId = fileId;
        this.filename = filename;
        this.fileSize = fileSize;
        this.mimeType = mimeType;
        this.createdAt = createdAt;
        this.b2FileId = b2FileId;
        this.b2FilePath = b2FilePath;
    }

    public String getFileId() { return fileId; }
    public void setFileId(String fileId) { this.fileId = fileId; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    
    public String getB2FileId() { return b2FileId; }
    public void setB2FileId(String b2FileId) { this.b2FileId = b2FileId; }
    
    public String getB2FilePath() { return b2FilePath; }
    public void setB2FilePath(String b2FilePath) { this.b2FilePath = b2FilePath; }
}
