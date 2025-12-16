package com.papairs.docs.controller;

import com.papairs.docs.dto.request.YjsDocumentRequest;
import com.papairs.docs.service.PermissionService;
import com.papairs.docs.service.YjsDocumentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for Y.js document management
 * Handles persistence of Y.js CRDT state
 */
@RestController
@RequestMapping("/api/docs/pages")
public class YjsController {
    
    private final YjsDocumentService yjsDocumentService;
    private final PermissionService permissionService;

    public YjsController(YjsDocumentService yjsDocumentService, PermissionService permissionService) {
        this.yjsDocumentService = yjsDocumentService;
        this.permissionService = permissionService;
    }

    /**
     * Validate user access to a page for collaboration
     * Used by collaboration-service to verify authentication and authorization
     * @param pageId page ID
     * @param userId user ID from authentication
     * @return user info if access is granted
     */
    @GetMapping("/{pageId}/yjs/validate")
    public ResponseEntity<Map<String, String>> validateAccess(
            @PathVariable String pageId,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-Internal-Service", required = false) String internalService
    ) {
        // Only allow access from collaboration-service
        if (!"collaboration-service".equals(internalService)) {
            return ResponseEntity.status(403).build();
        }

        if (userId == null || userId.isEmpty()) {
            return ResponseEntity.status(401).build();
        }

        try {
            // Check if user has access to the page
            if (!permissionService.canAccessPage(pageId, userId)) {
                return ResponseEntity.status(403).build();
            }

            // Return user info
            return ResponseEntity.ok(Map.of("userId", userId));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get Y.js document state for collaborative editing
     * @param pageId page ID
     * @return Y.js document state as byte array
     */
    @GetMapping(value = "/{pageId}/yjs", produces = "application/octet-stream")
    public ResponseEntity<byte[]> getYjsDocument(
            @PathVariable String pageId,
            @RequestHeader(value = "X-Internal-Service", required = false) String internalService
    ) {
        // Only allow access from collaboration-service or authenticated users
        if (!"collaboration-service".equals(internalService)) {
            return ResponseEntity.status(403).build();
        }

        try {
            byte[] ydoc = yjsDocumentService.getYjsDocument(pageId);
            return ResponseEntity.ok()
                    .header("Content-Type", "application/octet-stream")
                    .body(ydoc);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Save Y.js document state
     * @param pageId page ID
     * @param request Y.js document state
     * @return success response
     */
    @PostMapping("/{pageId}/yjs")
    public ResponseEntity<Void> saveYjsDocument(
            @PathVariable String pageId,
            @RequestBody YjsDocumentRequest request,
            @RequestHeader(value = "X-Internal-Service", required = false) String internalService
    ) {
        System.out.println("[YjsController] POST /yjs endpoint called for page: " + pageId);
        System.out.println("[YjsController] X-Internal-Service header: " + internalService);
        System.out.println("[YjsController] Request ydoc size: " + (request.getYdoc() != null ? request.getYdoc().size() : "null"));
        
        // Only allow access from collaboration-service
        if (!"collaboration-service".equals(internalService)) {
            System.out.println("[YjsController] ❌ Access denied - not from collaboration-service");
            return ResponseEntity.status(403).build();
        }

        try {
            yjsDocumentService.saveYjsDocument(pageId, request.getYdoc());
            System.out.println("[YjsController] ✅ Document saved successfully");
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            System.out.println("[YjsController] ❌ Error saving document: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get page HTML content for Y.js initialization
     * Used when migrating from HTML to Y.js format
     * Accessible by both internal services and authenticated users
     */
    @GetMapping("/{pageId}/content")
    public ResponseEntity<String> getPageContent(
            @PathVariable String pageId,
            @RequestHeader(value = "X-Internal-Service", required = false) String internalService,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        System.out.println("[YjsController] GET /content endpoint called for page: " + pageId);
        System.out.println("[YjsController] X-Internal-Service header: " + internalService);
        System.out.println("[YjsController] X-User-Id header: " + userId);
        
        // Allow access from collaboration-service OR from authenticated users
        boolean isInternalService = "collaboration-service".equals(internalService);
        boolean isAuthenticatedUser = userId != null && !userId.isEmpty();
        
        if (!isInternalService && !isAuthenticatedUser) {
            System.out.println("[YjsController] Access denied - neither internal service nor authenticated user");
            return ResponseEntity.status(403).build();
        }

        // If user request, check permissions
        if (!isInternalService && isAuthenticatedUser) {
            try {
                if (!permissionService.canAccessPage(pageId, userId)) {
                    System.out.println("[YjsController] Access denied - user doesn't have permission");
                    return ResponseEntity.status(403).build();
                }
            } catch (RuntimeException e) {
                System.out.println("[YjsController] Error checking permissions: " + e.getMessage());
                return ResponseEntity.notFound().build();
            }
        }

        try {
            String content = yjsDocumentService.getPageHtmlContent(pageId);
            System.out.println("[YjsController] Successfully retrieved content, length: " + content.length());
            return ResponseEntity.ok(content);
        } catch (RuntimeException e) {
            System.out.println("[YjsController] Error retrieving content: " + e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
}
