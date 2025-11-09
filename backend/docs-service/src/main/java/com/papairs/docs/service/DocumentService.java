package com.papairs.docs.service;

import com.papairs.docs.model.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service class for WebSocket document operations.
 * Acts as a bridge between WebSocket handlers and the PageService.
 * This service handles document content for real-time collaborative editing.
 * 
 * @author Papairs Team
 * @since 1.0
 */
@Service
public class DocumentService {

    private final PageService pageService;

    @Autowired
    public DocumentService(PageService pageService) {
        this.pageService = pageService;
    }

    /**
     * Save document content (called from WebSocket handler).
     * This method updates page content through the PageService.
     * 
     * @param docId the document/page ID as a string
     * @param content the document content to save
     * @param userId the user ID performing the save operation
     * @return the updated Page entity
     * @throws com.papairs.docs.exception.ResourceNotFoundException if page not found
     * @throws com.papairs.docs.exception.UnauthorizedAccessException if user lacks permission
     */
    public Page saveDocument(String docId, String content, String userId) {
        return pageService.updatePage(docId, userId, content);
    }

    /**
     * Get document content (for WebSocket initialization).
     * This method retrieves page content through the PageService.
     * 
     * @param docId the document/page ID as a string
     * @param userId the user ID requesting the content
     * @return the document content, or empty string if document not found or no access
     */
    public String getDocumentContent(String docId, String userId) {
        try {
            Page page = pageService.getPage(docId, userId);
            return page.getContent() != null ? page.getContent() : "";
        } catch (Exception e) {
            // Return empty content if page not found or access denied
            // This allows WebSocket to start with empty content for new documents
            return "";
        }
    }
}