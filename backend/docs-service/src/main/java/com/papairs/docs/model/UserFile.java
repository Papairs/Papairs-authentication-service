package com.papairs.docs.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_file")
public class UserFile {

    @Id
    @Column(name = "file_id", length = 36)
    private String fileId;

    @Column(name = "user_id", length = 36, nullable = false)
    private String userId;

    @Column(name = "filename", nullable = false)
    private String filename;

    @Column(name = "b2_file_id", nullable = false)
    private String b2FileId;

    @Column(name = "b2_file_path", length = 500, nullable = false)
    private String b2FilePath;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    // Constructors
    public UserFile() {}

    public UserFile(String fileId, String userId, String filename, String b2FileId, 
                    String b2FilePath, Long fileSize, String mimeType) {
        this.fileId = fileId;
        this.userId = userId;
        this.filename = filename;
        this.b2FileId = b2FileId;
        this.b2FilePath = b2FilePath;
        this.fileSize = fileSize;
        this.mimeType = mimeType;
    }

    // Getters and Setters
    public String getFileId() { return fileId; }
    public void setFileId(String fileId) { this.fileId = fileId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public String getB2FileId() { return b2FileId; }
    public void setB2FileId(String b2FileId) { this.b2FileId = b2FileId; }

    public String getB2FilePath() { return b2FilePath; }
    public void setB2FilePath(String b2FilePath) { this.b2FilePath = b2FilePath; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
