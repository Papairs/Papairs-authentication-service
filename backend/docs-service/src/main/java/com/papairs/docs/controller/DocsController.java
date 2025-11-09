package com.papairs.docs.controller;

import com.papairs.docs.model.Document;
import com.papairs.docs.model.ApiResponse;
import com.papairs.docs.service.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/docs")
@CrossOrigin(origins = "http://localhost:3000")
public class DocsController {

    private final DocumentService documentService;

    @Autowired
    public DocsController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping("/health")
    public ApiResponse health() {
        return new ApiResponse("success", "Docs service is running", 
                              Map.of("timestamp", LocalDateTime.now(),
                                     "service", "docs-service",
                                     "status", "healthy"));
    }

    @GetMapping("/all")
    public List<Document> getAllDocuments() {
        return documentService.getAllDocuments();
    }

    @GetMapping("/{id}")
    public ApiResponse getDocument(@PathVariable Long id) {
        return documentService.getDocumentById(id)
                .map(doc -> new ApiResponse("success", "Document retrieved successfully", doc))
                .orElse(new ApiResponse("error", "Document not found", null));
    }

    @PostMapping
    public ApiResponse createDocument(@RequestBody Document document) {
        Document savedDoc = documentService.createDocument(document);
        return new ApiResponse("success", "Document created successfully", savedDoc);
    }

    @PutMapping("/{id}")
    public ApiResponse updateDocument(@PathVariable Long id, @RequestBody Document document) {
        Document updatedDoc = documentService.updateDocument(id, document);
        return new ApiResponse("success", "Document updated successfully", updatedDoc);
    }

    @DeleteMapping("/{id}")
    public ApiResponse deleteDocument(@PathVariable Long id) {
        documentService.deleteDocument(id);
        return new ApiResponse("success", "Document deleted successfully", 
                              Map.of("deletedId", id));
    }
}