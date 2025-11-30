package com.papairs.docs.dto.request;

public class FileUploadRequest {
    private String userId;
    private String filename;
    private String base64Content;
    private String mimeType;

    public FileUploadRequest() {}

    public FileUploadRequest(String userId, String filename, String base64Content, String mimeType) {
        this.userId = userId;
        this.filename = filename;
        this.base64Content = base64Content;
        this.mimeType = mimeType;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public String getBase64Content() { return base64Content; }
    public void setBase64Content(String base64Content) { this.base64Content = base64Content; }

    public String getMimeType() { return mimeType != null ? mimeType : "text/plain"; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
}
