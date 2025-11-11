package com.papairs.docs.dto.request;

import com.papairs.docs.model.enums.MemberRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class AddPageMemberRequest {
    @NotBlank(message = "User ID is request")
    private String userId;

    @NotNull(message = "Role is required")
    private MemberRole role;

    public AddPageMemberRequest() {
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public MemberRole getRole() {
        return role;
    }

    public void setRole(MemberRole role) {
        this.role = role;
    }
}
