package com.papairs.docs.model;

import java.io.Serializable;
import java.util.Objects;

public class PageMemberId implements Serializable {
    private String pageId;
    private String userId;

    public PageMemberId() {
    }

    public PageMemberId(String pageId, String userId) {
        this.pageId = pageId;
        this.userId = userId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PageMemberId that = (PageMemberId) o;
        return Objects.equals(pageId, that.pageId) && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pageId, userId);
    }

    public String getPageId() {
        return pageId;
    }

    public void setPageId(String pageId) {
        this.pageId = pageId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
