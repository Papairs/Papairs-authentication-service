package com.papairs.docs.controller;

import com.backblaze.b2.client.exceptions.B2Exception;
import com.papairs.docs.dto.request.FileUploadRequest;
import com.papairs.docs.dto.response.UserFileResponse;
import com.papairs.docs.exception.FileAlreadyExistsException;
import com.papairs.docs.service.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = "http://localhost:3000")
public class FileController {

    private static final Logger log = LoggerFactory.getLogger(FileController.class);
    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping
    public ResponseEntity<?> uploadFile(@RequestBody FileUploadRequest request) {
        try {
            UserFileResponse response = fileService.uploadFile(request);
            return ResponseEntity.ok(response);
        } catch (FileAlreadyExistsException e) {
            log.warn("File already exists: {} for user: {}", request.getFilename(), request.getUserId());
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (B2Exception | IOException e) {
            log.error("Failed to upload file: {} for user: {}", request.getFilename(), request.getUserId(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to upload file: " + e.getMessage()));
        }
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<UserFileResponse>> getUserFiles(@PathVariable String userId) {
        List<UserFileResponse> files = fileService.getUserFiles(userId);
        return ResponseEntity.ok(files);
    }

    @DeleteMapping("/{fileId}")
    public ResponseEntity<?> deleteFile(@PathVariable String fileId, @RequestParam String userId) {
        try {
            fileService.deleteFile(fileId, userId);
            return ResponseEntity.ok(new SuccessResponse("File deleted successfully"));
        } catch (RuntimeException e) {
            log.warn("Failed to delete file {}: {}", fileId, e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (B2Exception | IOException e) {
            log.error("Error deleting file {}", fileId, e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to delete file: " + e.getMessage()));
        }
    }

    @GetMapping("/download/{fileId}")
    public ResponseEntity<?> downloadFile(@PathVariable String fileId, @RequestParam String userId) {
        try {
            byte[] fileContent = fileService.downloadFile(fileId, userId);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(fileContent);
        } catch (RuntimeException e) {
            log.warn("Failed to download file {}: {}", fileId, e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (B2Exception | IOException e) {
            log.error("Error downloading file {}", fileId, e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to download file: " + e.getMessage()));
        }
    }

    private static class ErrorResponse {
        private String error;

        public ErrorResponse(String error) {
            this.error = error;
        }

        public String getError() { return error; }
    }

    private static class SuccessResponse {
        private String message;

        public SuccessResponse(String message) {
            this.message = message;
        }

        public String getMessage() { return message; }
    }
}
