package com.papairs.docs.model.enums;

public enum MemberRole {
    OWNER("Owner", 3),
    EDITOR("Editor", 2),
    VIEWER("Viewer", 1);

    private final String roleName;
    private final int permissionLevel;

    /**
     * Constructor for MemberRole enum
     * @param roleName
     * @param permissionLevel
     */
    MemberRole(String roleName, int permissionLevel) {
        this.roleName = roleName;
        this.permissionLevel = permissionLevel;
    }

    /**
     * Get the role name
     * @return String
     */
    public String getRoleName() {
        return roleName;
    }

    /**
     * Get the numeric permission level
     * @return int
     */
    public int getPermissionLevel() {
        return permissionLevel;
    }

    /**
     * Check if the role can page content
     * @return true if role is EDITOR or OWNER
     */
    public boolean canEdit() {
        return this == EDITOR || this == OWNER;
    }

    /**
     * Check if the role can delete the page
     * @return true only if role is OWNER
     */
    public boolean canDelete() {
        return this == OWNER;
    }

    /**
     * Check if the role can manage page members (add/remove/change roles)
     * @return true only if role is OWNER
     */
    public boolean canManageMembers() {
        return this == OWNER;
    }

    /**
     * Check if the role can view the page
     * @return always true
     */
    public boolean canView() {
        return true;
    }

    /**
     * Check if this role has at least the permission level of another role
     * @param other the other MemberRole to compare against
     * @return true if this role >= permission level
     */
    public boolean hasPermissionLevel(MemberRole other) {
        return this.permissionLevel >= other.permissionLevel;
    }

    /**
     * Parse a string to a MemberRole (not case-sensitive)
     * @param roleString the role string
     * @return MemberRole or null if invalid
     */
    public static MemberRole fromString(String roleString) {
        if (roleString == null) {
            return null;
        }

        try {
            return MemberRole.valueOf(roleString.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Check if a string is a valid MemberRole
     * @param roleString the role string
     * @return true if valid, else false
     */
    public static boolean isValid(String roleString) {
        return fromString(roleString) != null;
    }
}
