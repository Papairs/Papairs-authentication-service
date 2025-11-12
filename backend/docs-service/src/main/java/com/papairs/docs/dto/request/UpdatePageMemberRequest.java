package com.papairs.docs.dto.request;

import com.papairs.docs.model.enums.MemberRole;
import jakarta.validation.constraints.NotNull;

public class UpdatePageMemberRequest {
    @NotNull(message = "Role is required")
    private MemberRole role;

    public UpdatePageMemberRequest() {
    }

    public MemberRole getRole() {
        return role;
    }

    public void setRole(MemberRole role) {
        this.role = role;
    }
}
