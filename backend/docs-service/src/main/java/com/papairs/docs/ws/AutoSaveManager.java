package com.papairs.docs.ws;

import com.papairs.docs.service.DocumentService;
import org.springframework.stereotype.Component;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Manages automatic saving of documents with configurable delay.
 * Provides timer-based auto-save functionality with conflict resolution.
 */
@Component
public class AutoSaveManager {
    private static final Logger logger = Logger.getLogger(AutoSaveManager.class.getName());
    private static final int SAVE_DELAY_SECONDS = 5;
    
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final DocumentService documentService;

    public AutoSaveManager(DocumentService documentService) {
        this.documentService = documentService;
    }

    /**
     * Schedules a delayed save operation.
     * Cancels any existing timer and creates a new one.
     */
    public void scheduleDelayedSave(DocumentSession document) {
        if (!document.isDirty()) {
            return;
        }

        // Cancel existing timer if present
        document.cancelSaveTimer();
        
        // Schedule new save timer
        var saveTimer = scheduler.schedule(
            () -> saveDocumentIfDirty(document), 
            SAVE_DELAY_SECONDS, 
            TimeUnit.SECONDS
        );
        
        document.setSaveTimer(saveTimer);
    }

    /**
     * Immediately saves a document if it has unsaved changes.
     */
    public void saveImmediately(DocumentSession document) {
        document.cancelSaveTimer();
        saveDocumentIfDirty(document);
    }

    /**
     * Performs the actual document save operation.
     */
    private void saveDocumentIfDirty(DocumentSession document) {
        if (!document.isDirty()) {
            return;
        }

        try {
            String userId = document.getAnyActiveUser();
            documentService.saveDocument(
                document.getDocumentId(), 
                document.getContent(), 
                userId
            );
            document.markClean();
            logger.info("Successfully saved document: " + document.getDocumentId());
        } catch (Exception e) {
            logger.log(Level.SEVERE, 
                "Failed to save document " + document.getDocumentId(), e);
        }
    }

    /**
     * Gracefully shuts down the auto-save scheduler.
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}