package com.papairs.docs.controller;

import com.papairs.docs.model.ApiResponse;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Health check controller for the docs service.
 * Page management endpoints have been moved to PageController.
 */
@RestController
@RequestMapping("/api/docs")
public class DocsController {

    @GetMapping("/health")
    public ApiResponse health() {
        return new ApiResponse("success", "Docs service is running", 
                              Map.of("timestamp", LocalDateTime.now(),
                                     "service", "docs-service",
                                     "status", "healthy"));
    }
}