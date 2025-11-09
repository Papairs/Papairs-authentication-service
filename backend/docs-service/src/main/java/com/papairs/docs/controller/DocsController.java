package com.papairs.docs.controller;

import com.papairs.docs.model.Page;
import com.papairs.docs.model.ApiResponse;
import com.papairs.docs.service.PageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * @deprecated This controller is deprecated. Use PageController instead for proper page management.
 * This controller will be removed in a future version.
 */
@Deprecated
@RestController
@RequestMapping("/api/docs")
@CrossOrigin(origins = "http://localhost:3000")
public class DocsController {

    private final PageService pageService;

    @Autowired
    public DocsController(PageService pageService) {
        this.pageService = pageService;
    }

    @GetMapping("/health")
    public ApiResponse health() {
        return new ApiResponse("success", "Docs service is running", 
                              Map.of("timestamp", LocalDateTime.now(),
                                     "service", "docs-service",
                                     "status", "healthy"));
    }

    /**
     * @deprecated Use PageController.getUserPages() instead
     */
    @Deprecated
    @GetMapping("/all")
    public List<Page> getAllDocuments() {
        // This is a simplified version - in reality, you'd need proper user authentication
        // For now, return empty list since we can't determine user without proper auth
        System.out.println("WARNING: /api/docs/all is deprecated. Use /api/docs/pages instead.");
        return List.of(); // Return empty list for safety
    }

    /**
     * @deprecated Use PageController.getPage() instead
     */
    @Deprecated
    @GetMapping("/{id}")
    public ApiResponse getDocument(@PathVariable Long id) {
        return new ApiResponse("error", "This endpoint is deprecated. Use /api/docs/pages/{pageId} instead.", 
                              Map.of("deprecatedEndpoint", "/api/docs/" + id,
                                     "newEndpoint", "/api/docs/pages/{pageId}"));
    }

    /**
     * @deprecated Use PageController.createPage() instead
     */
    @Deprecated
    @PostMapping
    public ApiResponse createDocument(@RequestBody Map<String, Object> document) {
        return new ApiResponse("error", "This endpoint is deprecated. Use POST /api/docs/pages instead.", 
                              Map.of("deprecatedEndpoint", "POST /api/docs",
                                     "newEndpoint", "POST /api/docs/pages"));
    }

    /**
     * @deprecated Use PageController.updatePage() instead
     */
    @Deprecated
    @PutMapping("/{id}")
    public ApiResponse updateDocument(@PathVariable Long id, @RequestBody Map<String, Object> document) {
        return new ApiResponse("error", "This endpoint is deprecated. Use PUT /api/docs/pages/{pageId} instead.", 
                              Map.of("deprecatedEndpoint", "PUT /api/docs/" + id,
                                     "newEndpoint", "PUT /api/docs/pages/{pageId}"));
    }

    /**
     * @deprecated Use PageController.deletePage() instead
     */
    @Deprecated
    @DeleteMapping("/{id}")
    public ApiResponse deleteDocument(@PathVariable Long id) {
        return new ApiResponse("error", "This endpoint is deprecated. Use DELETE /api/docs/pages/{pageId} instead.", 
                              Map.of("deprecatedEndpoint", "DELETE /api/docs/" + id,
                                     "newEndpoint", "DELETE /api/docs/pages/{pageId}"));
    }
}