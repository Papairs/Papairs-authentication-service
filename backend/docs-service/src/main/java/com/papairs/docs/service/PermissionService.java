package com.papairs.docs.service;

import com.papairs.docs.exception.ResourceNotFoundException;
import com.papairs.docs.exception.UnauthorizedAccessException;
import com.papairs.docs.model.Folder;
import com.papairs.docs.model.Page;
import com.papairs.docs.repository.FolderRepository;
import com.papairs.docs.repository.PageRepository;
import org.springframework.stereotype.Service;

@Service
public class PermissionService {
    private final FolderRepository folderRepository;
    private final PageRepository pageRepository;

    public PermissionService(FolderRepository folderRepository, PageRepository pageRepository) {
        this.folderRepository = folderRepository;
        this.pageRepository = pageRepository;
    }

    public void verifyFolderOwnership(String folderId, String userId) {
        if (folderId == null || folderId.isEmpty()) {
            return;
        }

        Folder folder = folderRepository.findById(folderId).orElseThrow(() -> new ResourceNotFoundException("Folder not found"));

        if (!folder.getOwnerId().equals(userId)) {
            throw new UnauthorizedAccessException("You don't have permission to access to this folder");
        }
    }

    public void verifyPageOwnership(String pageId, String userId) {
        Page page = pageRepository.findById(pageId).orElseThrow(() -> new ResourceNotFoundException("Page not found"));

        if (!page.getOwnerId().equals(userId)) {
            throw new UnauthorizedAccessException("You don't have permission to access to this folder");
        }
    }

    public Folder getFolderWithCheck(String folderId, String userId) {
        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new ResourceNotFoundException("Folder not found"));

        if (!folder.getOwnerId().equals(userId)) {
            throw new UnauthorizedAccessException("You don't own this folder");
        }

        return folder;
    }

    public Page getPageWithCheck(String pageId, String userId) {
        Page page = pageRepository.findById(pageId)
                .orElseThrow(() -> new ResourceNotFoundException("Page not found"));

        if (!page.getOwnerId().equals(userId)) {
            throw new UnauthorizedAccessException("You don't own this page");
        }

        return page;
    }
}
