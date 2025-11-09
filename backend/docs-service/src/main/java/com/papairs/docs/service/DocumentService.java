package com.papairs.docs.service;

import com.papairs.docs.model.Document;
import com.papairs.docs.repository.DocumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;

    @Autowired
    public DocumentService(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    // Save document content (called from WebSocket handler)
    public Document saveDocument(String docId, String content, String title) {
        try {
            Long id = Long.parseLong(docId);
            Optional<Document> existingDoc = documentRepository.findById(id);
            
            if (existingDoc.isPresent()) {
                Document doc = existingDoc.get();
                doc.setContent(content);
                return documentRepository.save(doc);
            } else {
                // Create new document
                Document newDoc = new Document();
                newDoc.setId(id);
                newDoc.setTitle(title != null ? title : "Untitled Document");
                newDoc.setContent(content);
                newDoc.setCreatedAt(LocalDateTime.now());
                return documentRepository.save(newDoc);
            }
        } catch (NumberFormatException e) {
            // Handle non-numeric docIds by creating new document
            Document newDoc = new Document();
            newDoc.setTitle(title != null ? title : docId);
            newDoc.setContent(content);
            newDoc.setCreatedAt(LocalDateTime.now());
            return documentRepository.save(newDoc);
        }
    }

    // Get document content (for WebSocket initialization)
    public String getDocumentContent(String docId) {
        try {
            Long id = Long.parseLong(docId);
            return documentRepository.findById(id)
                    .map(Document::getContent)
                    .orElse("");
        } catch (NumberFormatException e) {
            // For non-numeric IDs, return empty content
            return "";
        }
    }

    // Standard CRUD operations
    public List<Document> getAllDocuments() {
        return documentRepository.findAll();
    }

    public Optional<Document> getDocumentById(Long id) {
        return documentRepository.findById(id);
    }

    public Document createDocument(Document document) {
        document.setCreatedAt(LocalDateTime.now());
        return documentRepository.save(document);
    }

    public Document updateDocument(Long id, Document document) {
        document.setId(id);
        return documentRepository.save(document);
    }

    public void deleteDocument(Long id) {
        documentRepository.deleteById(id);
    }
}