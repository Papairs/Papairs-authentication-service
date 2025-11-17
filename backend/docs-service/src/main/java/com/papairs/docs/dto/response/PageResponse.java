package com.papairs.docs.dto.response;

import com.papairs.docs.model.Page;
import com.papairs.docs.model.enums.MemberRole;

import java.time.LocalDateTime;

/**
 * Response DTO for page data including the user's role
 */
public class PageResponse {
    private String pageId;
    private String title;
    private String folderId;
    private String ownerId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String userRole;  // OWNER, EDITOR, or VIEWER

    public PageResponse() {
    }

    public PageResponse(Page page, String userId, MemberRole memberRole) {
        this.pageId = page.getPageId();
        this.title = page.getTitle();
        this.folderId = page.getFolderId();
        this.ownerId = page.getOwnerId();
        this.createdAt = page.getCreatedAt();
        this.updatedAt = page.getUpdatedAt();
        
        // Determine user's role: OWNER takes precedence, otherwise use membership role
        if (page.getOwnerId().equals(userId)) {
            this.userRole = "OWNER";
        } else if (memberRole != null) {
            this.userRole = memberRole.name();  // EDITOR or VIEWER
        } else {
            this.userRole = null;
        }
    }

    // Getters and setters
    public String getPageId() {
        return pageId;
    }

    public void setPageId(String pageId) {
        this.pageId = pageId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getFolderId() {
        return folderId;
    }

    public void setFolderId(String folderId) {
        this.folderId = folderId;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getUserRole() {
        return userRole;
    }

    public void setUserRole(String userRole) {
        this.userRole = userRole;
    }
}
