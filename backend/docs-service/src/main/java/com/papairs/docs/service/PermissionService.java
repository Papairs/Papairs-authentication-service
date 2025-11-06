package com.papairs.docs.service;

import com.papairs.docs.exception.ResourceNotFoundException;
import com.papairs.docs.exception.UnauthorizedAccessException;
import com.papairs.docs.model.Page;
import com.papairs.docs.model.enums.MemberRole;
import com.papairs.docs.repository.FolderRepository;
import com.papairs.docs.repository.PageMemberRepository;
import com.papairs.docs.repository.PageRepository;
import org.springframework.stereotype.Service;

@Service
public class PermissionService {
    private final FolderRepository folderRepository;
    private final PageRepository pageRepository;
    private final PageMemberRepository pageMemberRepository;

    public PermissionService(FolderRepository folderRepository, PageRepository pageRepository, PageMemberRepository pageMemberRepository) {
        this.folderRepository = folderRepository;
        this.pageRepository = pageRepository;
        this.pageMemberRepository = pageMemberRepository;
    }

    /**
     * Checks if a user has access to a specific folder
     * A user has access if they are the owner of the folder
     * @param folderId The ID of the folder. A {@code null} ID assumes root access and returns {@code true}
     * @param userId The ID of the user
     * @return {@code true} if the user owns the folder, {@code false} otherwise
     */
    public boolean canAccessFolder(String folderId, String userId) {
        if (folderId == null) {
            return true;
        }

        return folderRepository.findById(folderId)
            .map(folder -> folder.getOwnerId().equals(userId))
            .orElse(false);
    }

    /**
     * Enforces folder access, throwing an exception if the user lacks permission
     * @param folderId The ID of the folder
     * @param userId The ID of the user
     * @throws UnauthorizedAccessException if the user does not have access.
     */
    public void requireFolderAccess(String folderId, String userId) {
        if (!canAccessFolder(folderId, userId)) {
            throw new UnauthorizedAccessException("You don't have access to this folder");
        }
    }

    /**
     * Checks if a user can view a page
     * Access is granted if the user is the owner or a member (either {@code EDITOR} or {@code VIEWER})
     * @param pageId The ID of the page
     * @param userId The ID of the user
     * @return {@code true} if the user can access the page, {@code false} otherwise
     */
    public boolean canAccessPage(String pageId, String userId) {
        return pageRepository.findById(pageId)
            .map(page -> isOwner(page, userId) || pageMemberRepository.existsByPageIdAndUserId(pageId, userId))
            .orElse(false);
    }

    /**
     * Checks if a user can edit a page
     * Permission is granted if the user is the owner or a member with the {@code EDITOR} role
     * @param pageId The ID of the page
     * @param userId The ID of the user
     * @return {@code true} if the user has edit permissions, {@code false} otherwise.
     */
    public boolean canEditPage(String pageId, String userId) {
        Page page = pageRepository.findById(pageId)
            .orElseThrow(() -> new ResourceNotFoundException("Page not found."));

        if (isOwner(page, userId)) {
            return true;
        }

        return pageMemberRepository.existsByPageIdAndUserIdAndRole(pageId, userId, MemberRole.EDITOR);
    }

    /**
     * Checks if a user can manage members of a page (add, remove, or change roles)
     * This is restricted to the page owner or members with the {@code EDITOR} role
     * Logic is currently the same as edit permissions
     * @param pageId The ID of the page
     * @param userId The ID of the user
     * @return {@code true} if the user can manage members, {@code false} otherwise.
     */
    public boolean canManageMembers(String pageId, String userId) {
        return canEditPage(pageId, userId);
    }

    /**
     * Enforces page access, throwing an exception if the user lacks permission
     * @param pageId The ID of the page
     * @param userId The ID of the user
     * @throws UnauthorizedAccessException if the user does not have access
     */
    public void requirePageAccess(String pageId, String userId) {
        if (!canAccessPage(pageId, userId)) {
            throw new UnauthorizedAccessException("You don't have access to this page");
        }
    }

    /**
     * Enforces page edit permissions, throwing an exception if the user cannot edit
     * @param pageId The ID of the page
     * @param userId The ID of the user
     * @throws UnauthorizedAccessException if the user lacks edit permissions
     */
    public void requirePageEdit(String pageId, String userId) {
        if (!canEditPage(pageId, userId)) {
            throw new UnauthorizedAccessException("You don't have edit permission");
        }
    }

    /**
     * Enforces member management permissions, throwing an exception if the user is unauthorized
     * @param pageId The ID of the page
     * @param userId The ID of the user
     * @throws UnauthorizedAccessException if the user cannot manage members
     */
    public void requireMemberManagement(String pageId, String userId) {
        if (!canManageMembers(pageId, userId)) {
            throw new UnauthorizedAccessException("Only owner can manage members");
        }
    }

    /**
     * Determines if a user is the owner of a page
     * @param pageId The ID of the page.
     * @param userId The ID of the user.
     * @return true if the user is the owner, false otherwise.
     */
    public boolean isOwner(String pageId, String userId) {
        return pageRepository.findById(pageId)
            .map(page -> page.getOwnerId().equals(userId))
            .orElse(false);
    }

    /**
     * Overloaded helper method to check ownership from an existing Page object,
     * preventing an additional database query
     */
    private boolean isOwner(Page page, String userId) {
        return page.getOwnerId().equals(userId);
    }
}
