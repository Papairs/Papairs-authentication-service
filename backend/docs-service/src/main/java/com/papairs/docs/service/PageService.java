package com.papairs.docs.service;

import com.papairs.docs.exception.ResourceNotFoundException;
import com.papairs.docs.model.Page;
import com.papairs.docs.repository.PageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class PageService {
    private final PageRepository pageRepository;
    public PageService(PageRepository pageRepository) {
        this.pageRepository = pageRepository;
    }

    /**
     * Create a new page
     * @param title page title
     * @param ownerId owner's user ID
     * @return Created Page entity
     */
    @Transactional
    public Page createPage(String title, String ownerId, String folderId) {
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
    public Page updatePage(String pageId, String content) {
        Page page = getPage(pageId);
        page.setContent(content);
        return pageRepository.save(page);
    }

    @Transactional
    public Page renamePage(String pageId, String newTitle) {
        Page page = getPage(pageId);
        page.setTitle(newTitle);
        return pageRepository.save(page);
    }

    /**
     * Get a page by ID
     * @param pageId page ID
     * @return Page entity
     */
    public Page getPage(String pageId) {
        return pageRepository.findById(pageId)
                .orElseThrow(() -> new ResourceNotFoundException("Page not found"));
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
    public void deletePage(String pageId) {
        pageRepository.deleteById(pageId);
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
    public Page movePage(String pageId, String targetFolderId) {
        Page page = getPage(pageId);
        page.setFolderId(targetFolderId);
        return pageRepository.save(page);
    }
}
