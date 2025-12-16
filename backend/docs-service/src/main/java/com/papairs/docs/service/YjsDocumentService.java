package com.papairs.docs.service;

import com.papairs.docs.model.Page;
import com.papairs.docs.repository.PageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class YjsDocumentService {

    @Autowired
    private PageRepository pageRepository;
    
    @Autowired
    private PageService pageService;

    /**
     * Get Y.js document state for a page
     * If no Y.js state exists, return the HTML content wrapped in a special marker
     * so the collaboration service can initialize Y.js with the HTML
     */
    public byte[] getYjsDocument(String pageId) {
        Page page = pageRepository.findById(pageId)
            .orElseThrow(() -> new RuntimeException("Page not found: " + pageId));
        
        byte[] yjsState = page.getYjsState();
        
        if (yjsState == null || yjsState.length == 0) {
            return new byte[0];
        }
        return yjsState;
    }

    /**
     * Save Y.js document state for a page
     * Y.js state is now the source of truth for document content
     * HTML content field is no longer used for editing, only for backwards compatibility
     */
    @Transactional
    public void saveYjsDocument(String pageId, List<Integer> ydocIntegers) {
        byte[] yjsState = convertIntegerListToByteArray(ydocIntegers);
        pageService.updateYjsState(pageId, yjsState);
    }

    /**
     * Get page HTML content for Y.js initialization
     * Used when migrating existing documents to Y.js format
     */
    public String getPageHtmlContent(String pageId) {
        Page page = pageRepository.findById(pageId)
            .orElseThrow(() -> new RuntimeException("Page not found: " + pageId));
        
        String content = page.getContent();
        return content != null ? content : "";
    }

    /**
     * Convert List<Integer> from JavaScript to byte array
     * JavaScript sends Uint8Array which JSON serializes as array of integers
     */
    private byte[] convertIntegerListToByteArray(List<Integer> integers) {
        byte[] bytes = new byte[integers.size()];
        for (int i = 0; i < integers.size(); i++) {
            bytes[i] = integers.get(i).byteValue();
        }
        return bytes;
    }
}
