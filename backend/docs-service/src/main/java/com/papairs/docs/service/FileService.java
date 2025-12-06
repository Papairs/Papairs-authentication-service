package com.papairs.docs.service;

import com.backblaze.b2.client.B2StorageClient;
import com.backblaze.b2.client.contentSources.B2ByteArrayContentSource;
import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.structures.B2Bucket;
import com.backblaze.b2.client.structures.B2FileVersion;
import com.backblaze.b2.client.structures.B2UploadFileRequest;
import com.papairs.docs.dto.request.FileUploadRequest;
import com.papairs.docs.dto.response.UserFileResponse;
import com.papairs.docs.exception.FileAlreadyExistsException;
import com.papairs.docs.model.UserFile;
import com.papairs.docs.repository.UserFileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FileService {

    private static final Logger log = LoggerFactory.getLogger(FileService.class);
    private static final String STORAGE_MODE_LOCAL = "local";
    private static final String STORAGE_MODE_B2 = "b2";

    private final B2StorageClient b2Client;
    private final UserFileRepository fileRepository;
    private final UserService userService;
    
    @Value("${storage.mode:local}")
    private String storageMode;
    
    @Value("${storage.local-path:./uploads}")
    private String localStoragePath;
    
    @Value("${b2.bucket-name}")
    private String bucketName;
    
    private String bucketId;

    public FileService(B2StorageClient b2Client, UserFileRepository fileRepository, UserService userService) {
        this.b2Client = b2Client;
        this.fileRepository = fileRepository;
        this.userService = userService;
    }
    
    @PostConstruct
    public void init() throws B2Exception, IOException {
        log.info("Initializing file storage with mode: {}", storageMode);
        
        if (STORAGE_MODE_LOCAL.equals(storageMode)) {
            initializeLocalStorage();
        } else if (STORAGE_MODE_B2.equals(storageMode)) {
            initializeB2Storage();
        } else {
            log.warn("Unknown storage mode '{}', defaulting to local", storageMode);
            storageMode = STORAGE_MODE_LOCAL;
            initializeLocalStorage();
        }
    }
    
    private void initializeLocalStorage() throws IOException {
        Path uploadPath = Paths.get(localStoragePath);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
            log.info("Created local storage directory: {}", uploadPath.toAbsolutePath());
        } else {
            log.info("Using local storage directory: {}", uploadPath.toAbsolutePath());
        }
    }
    
    private void initializeB2Storage() throws B2Exception {
        List<B2Bucket> buckets = b2Client.buckets();
        log.debug("Found {} B2 bucket(s)", buckets.size());
        
        this.bucketId = buckets.stream()
                .filter(bucket -> bucket.getBucketName().equals(bucketName))
                .findFirst()
                .map(B2Bucket::getBucketId)
                .orElseThrow(() -> new RuntimeException(
                    String.format("B2 bucket '%s' not found. Available buckets: %s", 
                        bucketName, 
                        buckets.stream().map(B2Bucket::getBucketName).collect(Collectors.joining(", ")))));
        
        log.info("Using B2 bucket: {} (ID: {})", bucketName, bucketId);
    }

    @Transactional
    public UserFileResponse uploadFile(FileUploadRequest request) throws B2Exception, IOException {
        userService.ensureUserExists(request.getUserId(), null);
        
        if (fileRepository.existsByUserIdAndFilename(request.getUserId(), request.getFilename())) {
            throw new FileAlreadyExistsException("File '" + request.getFilename() + "' already exists");
        }

        byte[] fileContent = Base64.getDecoder().decode(request.getBase64Content());
        String fileId = UUID.randomUUID().toString();
        String filePath = buildFilePath(request.getUserId(), fileId, request.getFilename());
        
        String storageFileId = STORAGE_MODE_LOCAL.equals(storageMode)
                ? uploadToLocalStorage(fileContent, request.getUserId(), fileId, request.getFilename())
                : uploadToB2Storage(fileContent, filePath, request.getMimeType());

        UserFile userFile = new UserFile(
                fileId,
                request.getUserId(),
                request.getFilename(),
                storageFileId,
                filePath,
                (long) fileContent.length,
                request.getMimeType()
        );
        userFile.setCreatedAt(LocalDateTime.now());
        
        UserFile savedFile = fileRepository.save(userFile);
        log.info("File uploaded successfully: {} (size: {} bytes)", request.getFilename(), fileContent.length);
        
        return mapToResponse(savedFile);
    }
    
    private String buildFilePath(String userId, String fileId, String filename) {
        return String.format("%s/%s/%s", userId, fileId, filename);
    }
    
    private String uploadToLocalStorage(byte[] content, String userId, String fileId, String filename) throws IOException {
        Path userDir = Paths.get(localStoragePath, userId, fileId);
        Files.createDirectories(userDir);
        Path localFile = userDir.resolve(filename);
        Files.write(localFile, content);
        log.debug("Saved file locally: {}", localFile.toAbsolutePath());
        return "local_" + fileId;
    }
    
    private String uploadToB2Storage(byte[] content, String filePath, String mimeType) throws B2Exception {
        B2UploadFileRequest uploadRequest = B2UploadFileRequest
                .builder(bucketId, filePath, mimeType, B2ByteArrayContentSource.build(content))
                .build();
        
        B2FileVersion fileVersion = b2Client.uploadSmallFile(uploadRequest);
        log.debug("Uploaded file to B2: {}", filePath);
        return fileVersion.getFileId();
    }

    @Transactional(readOnly = true)
    public List<UserFileResponse> getUserFiles(String userId) {
        return fileRepository.findByUserId(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteFile(String fileId, String userId) throws B2Exception, IOException {
        UserFile file = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found"));

        if (!file.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized to delete this file");
        }

        if (STORAGE_MODE_LOCAL.equals(storageMode)) {
            deleteFromLocalStorage(file);
        } else {
            deleteFromB2Storage(file);
        }

        fileRepository.delete(file);
        log.info("File deleted: {} (fileId: {})", file.getFilename(), fileId);
    }
    
    private void deleteFromLocalStorage(UserFile file) throws IOException {
        Path localFile = Paths.get(localStoragePath, file.getB2FilePath());
        if (Files.exists(localFile)) {
            Files.delete(localFile);
            cleanupEmptyDirectories(localFile);
        }
        log.debug("Deleted local file: {}", localFile);
    }
    
    private void cleanupEmptyDirectories(Path file) throws IOException {
        Path parentDir = file.getParent();
        if (parentDir != null && Files.list(parentDir).findAny().isEmpty()) {
            Files.delete(parentDir);
            Path grandParentDir = parentDir.getParent();
            if (grandParentDir != null && Files.list(grandParentDir).findAny().isEmpty()) {
                Files.delete(grandParentDir);
            }
        }
    }
    
    private void deleteFromB2Storage(UserFile file) throws B2Exception {
        b2Client.deleteFileVersion(file.getB2FilePath(), file.getB2FileId());
        log.debug("Deleted B2 file: {}", file.getB2FilePath());
    }

    public byte[] downloadFile(String fileId, String userId) throws B2Exception, IOException {
        UserFile file = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found"));

        if (!file.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized to access this file");
        }

        byte[] content = STORAGE_MODE_LOCAL.equals(storageMode)
                ? downloadFromLocalStorage(file)
                : downloadFromB2Storage(file);
        
        log.debug("Downloaded file: {} ({} bytes)", file.getFilename(), content.length);
        return content;
    }
    
    private byte[] downloadFromLocalStorage(UserFile file) throws IOException {
        Path localFile = Paths.get(localStoragePath, file.getB2FilePath());
        if (!Files.exists(localFile)) {
            throw new IOException("File not found in local storage: " + file.getB2FilePath());
        }
        return Files.readAllBytes(localFile);
    }
    
    private byte[] downloadFromB2Storage(UserFile file) throws B2Exception {
        com.backblaze.b2.client.structures.B2DownloadByIdRequest downloadRequest = 
                com.backblaze.b2.client.structures.B2DownloadByIdRequest.builder(file.getB2FileId()).build();
        
        com.backblaze.b2.client.contentHandlers.B2ContentMemoryWriter writer = 
                com.backblaze.b2.client.contentHandlers.B2ContentMemoryWriter.build();
        
        b2Client.downloadById(downloadRequest, writer);
        return writer.getBytes();
    }

    private UserFileResponse mapToResponse(UserFile file) {
        return new UserFileResponse(
                file.getFileId(),
                file.getFilename(),
                file.getFileSize(),
                file.getMimeType(),
                file.getCreatedAt().toString(),
                file.getB2FileId(),
                file.getB2FilePath()
        );
    }
}
