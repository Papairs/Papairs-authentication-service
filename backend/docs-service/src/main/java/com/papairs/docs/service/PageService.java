package com.papairs.docs.service;

import com.papairs.docs.model.Page;
import com.papairs.docs.repository.PageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class PageService {
    private final PageRepository pageRepository;
    private final PermissionService permissionService;

    public PageService(PageRepository pageRepository, PermissionService permissionService) {
        this.pageRepository = pageRepository;
        this.permissionService = permissionService;
    }

    /**
     * Create a new page
     * @param title page title
     * @param ownerId owner's user ID
     * @return Created Page entity
     */
    @Transactional
    public Page createPage(String title, String ownerId, String folderId) {
        permissionService.verifyFolderOwnership(folderId, ownerId);

        Page page = new Page();
        page.setPageId(UUID.randomUUID().toString());
        page.setTitle(title);
        page.setFolderId(folderId);
        page.setOwnerId(ownerId);
        page.setContent("");
        return pageRepository.save(page);
    }

    /**
     * Update page content
     * @param pageId page ID
     * @param content new content
     * @return Updated Page entity
     */
    @Transactional
    public Page updatePage(String pageId, String userId, String content) {
        Page page = permissionService.getPageWithCheck(pageId, userId);
        page.setContent(content);
        return pageRepository.save(page);
    }

    /**
     * Rename a page
     * @param pageId page ID
     * @param userId user ID
     * @param newTitle new title
     * @return Updated Page entity
     */
    @Transactional
    public Page renamePage(String pageId, String userId, String newTitle) {
        Page page = permissionService.getPageWithCheck(pageId, userId);
        page.setTitle(newTitle);
        return pageRepository.save(page);
    }

    /**
     * Get a page by ID
     * @param pageId page ID
     * @return Page entity
     */
    public Page getPage(String pageId, String userId) {
        return permissionService.getPageWithCheck(pageId, userId);
    }

    /**
     * Get all pages for a user
     * @param userId user ID
     * @return List of Page entities
     */
    public List<Page> getUserPages(String userId) {
        return pageRepository.findByOwnerId(userId);
    }

    /**
     * Delete a page by ID
     * @param pageId page ID
     */
    @Transactional
    public void deletePage(String pageId, String userId) {
        Page page = permissionService.getPageWithCheck(pageId, userId);
        pageRepository.delete(page);
    }

    /**
     * Check if a folder has any pages
     * @param folderId folder ID
     * @return true if folder has pages, else false
     */
    public boolean folderHasPages(String folderId) {
        return pageRepository.existsByFolderId(folderId);
    }

    /**
     * Get page count in a folder
     * @param folderId folder ID
     * @return number of pages in the folder
     */
    public long getPageCountInFolder(String folderId) {
        return pageRepository.countByFolderId(folderId);
    }

    /**
     * Delete all pages in a folder
     * @param folderId folder ID
     */
    @Transactional
    public void deleteAllPagesInFolder(String folderId) {
        pageRepository.deleteByFolderId(folderId);
    }

    /**
     * Move a page to a different folder
     * @param pageId page ID
     * @param targetFolderId target folder ID
     * @return Updated Page entity
     */
    @Transactional
    public Page movePage(String pageId, String targetFolderId, String userId) {
        Page page = permissionService.getPageWithCheck(pageId, userId);
        permissionService.verifyFolderOwnership(targetFolderId, userId);

        page.setFolderId(targetFolderId);
        return pageRepository.save(page);
    }
}
