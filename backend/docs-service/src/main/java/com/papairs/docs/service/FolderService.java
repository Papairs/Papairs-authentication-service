package com.papairs.docs.service;

import com.papairs.docs.exception.InvalidRequestException;
import com.papairs.docs.exception.ResourceConflictException;
import com.papairs.docs.exception.ResourceNotFoundException;
import com.papairs.docs.model.Folder;
import com.papairs.docs.model.FolderTree;
import com.papairs.docs.repository.FolderRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import com.papairs.docs.model.Page;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FolderService {
    private final FolderRepository folderRepository;
    private final PermissionService permissionService;
    private final ContentService contentService;

    public FolderService(FolderRepository folderRepository, PermissionService permissionService, ContentService contentService) {
        this.folderRepository = folderRepository;
        this.permissionService = permissionService;
        this.contentService = contentService;
    }

    /**
     * Creates a new folder
     * The user must have access to the parent folder, if one is specified
     * @param name The name for the new folder
     * @param ownerId The ID of the user who will own the folder
     * @param parentFolderId The ID of the parent folder. Can be {@code null} to create a root folder.
     * @return The newly created {@link Folder} entity
     */
    @Transactional
    public Folder createFolder(String name, String ownerId, String parentFolderId) {
        if (parentFolderId != null) {
            permissionService.requireFolderAccess(parentFolderId, ownerId);
        }

        Folder folder = new Folder();
        folder.setName(name);
        folder.setOwnerId(ownerId);
        folder.setParentFolderId(parentFolderId);

        return folderRepository.save(folder);
    }

    /**
     * Retrieves a single folder by its ID by ensuring the user has access
     * @param folderId The ID of the folder to retrieve
     * @param userId The ID of the user making the request
     * @return The requested {@link Folder} entity
     * @throws ResourceNotFoundException if the folder is not found
     */
    public Folder getFolder(String folderId, String userId) {
        permissionService.requireFolderAccess(folderId, userId);

        return folderRepository.findById(folderId)
            .orElseThrow(() -> new ResourceNotFoundException("Folder not found"));
    }

    /**
     * Renames an existing folder
     * @param folderId The ID of the folder to rename
     * @param userId The ID of the user performing the action.
     * @param newName The new name for the folder
     * @return The updated {@link Folder} entity
     * @throws ResourceNotFoundException if the folder is not found
     */
    @Transactional
    public Folder renameFolder(String folderId, String userId, String newName) {
        Folder folder = getFolder(folderId, userId);
        folder.setName(newName);

        return folderRepository.save(folder);
    }

    /**
     * Deletes a folder
     * Fails if the folder is not empty, unless recursive delete is specified
     * @param folderId The ID of the folder to delete
     * @param userId The ID of the user performing the action
     * @param recursive If {@code true}, all subfolders and pages will be deleted
     *                  If {@code false}, the operation will fail if the folder is not empty
     * @throws ResourceNotFoundException   if the folder does not exist
     * @throws ResourceConflictException if {@code recursive} is false and the folder contains content
     */
    @Transactional
    public void deleteFolder(String folderId, String userId, boolean recursive) {
        permissionService.requireFolderAccess(folderId, userId);

        if (!folderRepository.existsById(folderId)) {
            throw new ResourceNotFoundException("Folder not found");
        }

        if (recursive) {
            contentService.deleteFolderWithContents(folderId);
        } else {
            if (contentService.folderHasContent(folderId)) {
                throw new ResourceConflictException("Folder is not empty");
            }
            folderRepository.deleteById(folderId);
        }
    }

    /**
     * Retrieves all root-level folders for a given user
     * @param userId The ID of the user
     * @return A {@link List} of root {@link Folder} entities
     */
    public List<Folder> getRootFolders(String userId) {
        return folderRepository.findByOwnerIdAndParentFolderIdIsNull(userId);
    }

    /**
     * Get all folders for a user
     * @param userId The ID of the user
     * @return A {@link List} of child {@link Folder} entities
     */
    public List<Folder> getAllUserFolders(String userId) {
        return folderRepository.findByOwnerId(userId);
    }

    /**
     * Retrieves all direct child folders of a given parent folder
     * @param folderId The ID of the parent folder
     * @param userId The ID of the user requesting the children
     * @return A {@link List} of child {@link Folder} entities
     */
    public List<Folder> getChildFolders(String folderId, String userId) {
        permissionService.requireFolderAccess(folderId, userId);
        return folderRepository.findByParentFolderId(folderId);
    }

    /**
     * Get folder tree starting from a folder
     * Uses a single recursive query to fetch all descendants at once,
     * then builds the tree in memory without additional database calls.
     * @param folderId folder ID
     * @param userId user ID
     * @return FolderTree structure
     * @throws ResourceNotFoundException if the folder does not exist
     */
    public FolderTree getFolderTree(String folderId, String userId) {
        permissionService.requireFolderAccess(folderId, userId);

        // Get folder and descendants
        List<String> descendantIds = folderRepository.findAllDescendantIds(folderId);
        List<String> allFolderIds = new ArrayList<>(descendantIds);
        allFolderIds.add(folderId);

        // Build tree structure
        List<Folder> allFolders = folderRepository.findAllById(allFolderIds);
        if (CollectionUtils.isEmpty(allFolders)) {
            throw new ResourceNotFoundException("Folder not found, or its descendants are missing");
        }

        // Get page counts from ContentService
        Folder rootFolder = allFolders.stream()
            .filter(f -> f.getFolderId().equals(folderId))
            .findFirst()
            .orElseThrow(() -> new ResourceNotFoundException("Folder not found"));

        Map<String, List<Folder>> childrenMap = allFolders.stream()
            .filter(f -> f.getParentFolderId() != null)
            .collect(Collectors.groupingBy(Folder::getParentFolderId));

        // Get page counts for all relevant folders in another single batch query.
        Map<String, Long> pageCountMap = contentService.getPageCountsForFolders(allFolderIds);

        // Build and return the tree structure recursively in memory.
        return buildFolderTree(rootFolder, childrenMap, pageCountMap, null);
    }

    /**
     * Build folder tree recursively using pre-fetched data
     * No database queries - uses in-memory maps
     * @param folder folder entity
     * @param childrenMap map of parent folder ID to list of child folders
     * @param pageCountMap map of folder ID to page count
     * @return FolderTree structure
     */
    private FolderTree buildFolderTree(
        Folder folder,
        Map<String, List<Folder>> childrenMap,
        Map<String, Long> pageCountMap,
        Map<String, List<Page>> pagesByFolderId
) {
    FolderTree tree = new FolderTree();

    tree.setFolderId(folder.getFolderId());
    tree.setName(folder.getName());
    tree.setCreatedAt(folder.getCreatedAt());

    long pageCount = pageCountMap.getOrDefault(folder.getFolderId(), 0L);
    tree.setPageCount(pageCount);

    // Add documents
    List<Page> documents = pagesByFolderId.getOrDefault(folder.getFolderId(), Collections.emptyList());
    tree.setDocuments(documents);

    // Build children
    List<Folder> children = childrenMap.getOrDefault(folder.getFolderId(), new ArrayList<>());
    List<FolderTree> childTrees = new ArrayList<>();

    for (Folder child : children) {
        childTrees.add(buildFolderTree(child, childrenMap, pageCountMap, pagesByFolderId));
    }

    tree.setChildren(childTrees);
    tree.setChildCount(childTrees.size());

    return tree;
    }

    /**
     * Constructs and retrieves the entire hierarchical tree of all folders owned by a user.
     * @param userId The ID of the user.
     * @return A {@link List} of {@link FolderTree} objects, one for each root folder.
     */

     public List<FolderTree> getUserFolderTrees(String userId) {
        List<Folder> allFolders = folderRepository.findByOwnerId(userId);
        if (CollectionUtils.isEmpty(allFolders)) {
            return Collections.emptyList();
        }
    
        List<Folder> rootFolders = allFolders.stream()
            .filter(f -> f.getParentFolderId() == null)
            .collect(Collectors.toList());
    
        Map<String, List<Folder>> childrenMap = allFolders.stream()
            .filter(f -> f.getParentFolderId() != null)
            .collect(Collectors.groupingBy(Folder::getParentFolderId));
    
        List<String> folderIds = allFolders.stream()
            .map(Folder::getFolderId)
            .collect(Collectors.toList());
    
        Map<String, Long> pageCountMap = contentService.getPageCountsForFolders(folderIds);
    
        Map<String, List<Page>> pagesByFolderId =
            contentService.getPagesForFolders(folderIds);
    
        System.out.println("Pages by Folder ID: " + pagesByFolderId);


        return rootFolders.stream()
            .map(root -> buildFolderTree(root, childrenMap, pageCountMap, pagesByFolderId))
            .collect(Collectors.toList());
        
    }
    

    /**
     * Move a folder to a new parent folder
     * @param folderId folder ID
     * @param userId user ID
     * @param newParentFolderId new parent folder ID
     * @return Updated Folder entity
     * @throws InvalidRequestException if attempting to move a folder into itself or its descendants
     */
    @Transactional
    public Folder moveFolder(String folderId, String userId, String newParentFolderId) {
        Folder folderToMove = getFolder(folderId, userId);

        if (newParentFolderId != null) {
            permissionService.requireFolderAccess(newParentFolderId, userId);

            if (folderId.equals(newParentFolderId)) {
                throw new InvalidRequestException("Cannot move a folder into itself");
            }

            List<String> descendantIds = folderRepository.findAllDescendantIds(folderId);
            if (descendantIds.contains(newParentFolderId)) {
                throw new InvalidRequestException("Cannot move a folder into one of its own descendants");
            }
        }

        folderToMove.setParentFolderId(newParentFolderId);
        return folderRepository.save(folderToMove);
    }

    /**
     * Retrieves the ancestor path from the root down to the specified folder
     * @param folderId The ID of the target folder
     * @param userId The ID of the user making the request
     * @return A {@link List} of {@link Folder} entities representing the path, ordered from root to target
     */
    public List<Folder> getFolderPath(String folderId, String userId) {
        permissionService.requireFolderAccess(folderId, userId);
        List<Folder> path = folderRepository.findAncestorPath(folderId);
        Collections.reverse(path); // Reverse to get root -> target order
        return path;
    }
}
