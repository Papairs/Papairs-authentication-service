package com.papairs.docs.service;

import com.papairs.docs.exception.InvalidRequestException;
import com.papairs.docs.exception.ResourceConflictException;
import com.papairs.docs.model.Folder;
import com.papairs.docs.model.FolderTree;
import com.papairs.docs.repository.FolderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class FolderService {
    private final FolderRepository folderRepository;
    private final PermissionService permissionService;
    private final PageService pageService;

    public FolderService(FolderRepository folderRepository, PermissionService permissionService, PageService pageService) {
        this.folderRepository = folderRepository;
        this.permissionService = permissionService;
        this.pageService = pageService;
    }

    /**
     * Create a new folder
     * @param name folder name
     * @param ownerId owner's user ID
     * @param parentFolderId parent folder ID (nullable)
     * @return Created Folder entity
     */
    @Transactional
    public Folder createFolder(String name, String ownerId, String parentFolderId) {
        if (parentFolderId != null && !parentFolderId.isEmpty()) {
            permissionService.verifyFolderOwnership(parentFolderId, ownerId);
        }

        Folder folder = new Folder();
        folder.setName(name);
        folder.setOwnerId(ownerId);
        folder.setParentFolderId(parentFolderId);

        return folderRepository.save(folder);
    }

    /**
     * Get folder by ID
     * @param folderId folder ID
     * @param userId user ID
     * @return Folder entity
     */
    public Folder getFolder(String folderId, String userId) {
        return permissionService.getFolderWithCheck(folderId, userId);
    }

    /**
     * Rename a folder
     * @param folderId folder ID
     * @param newName new folder name
     * @return Updated Folder entity
     */
    @Transactional
    public Folder renameFolder(String folderId, String userId, String newName) {
        Folder folder = permissionService.getFolderWithCheck(folderId, userId);
        folder.setName(newName);

        return folderRepository.save(folder);
    }

    /**
     * Delete a folder
     * @param folderId folder ID
     * @param userId user ID
     */
    @Transactional
    public void deleteFolder(String folderId, String userId) {
        Folder folder = permissionService.getFolderWithCheck(folderId, userId);

        if (folderRepository.existsByParentFolderId(folderId)) {
            throw new ResourceConflictException("Cannot delete folder with subfolders");
        }

        if (pageService.folderHasPages(folderId)) {
            throw new ResourceConflictException("Cannot delete folder with pages");
        }

        folderRepository.delete(folder);
    }

    /**
     * Delete a folder and all its subfolders and pages recursively
     * @param folderId folder ID
     * @param userId user ID
     */
    @Transactional
    public void deleteFolderRecursive(String folderId, String userId) {
        Folder folder = permissionService.getFolderWithCheck(folderId, userId);
        deleteFolderAndChildren(folder);
    }

    /**
     * Helper method to delete folder and its children recursively
     * @param folder folder to delete
     */
    private void deleteFolderAndChildren(Folder folder) {
        List<Folder> children = folderRepository.findByParentFolderId(folder.getFolderId());
        for (Folder child : children) {
            deleteFolderAndChildren(child);
        }

        pageService.deleteAllPagesInFolder(folder.getFolderId());

        folderRepository.delete(folder);
    }

    /**
     * Get root folders for a user
     * @param userId user ID
     * @return List of root Folder entities
     */
    public List<Folder> getRootFolders(String userId) {
        return folderRepository.findByOwnerIdAndParentFolderIdIsNull(userId);
    }

    /**
     * Get all folders for a user
     * @param userId user ID
     * @return List of Folder entities
     */
    public List<Folder> getAllUserFolders(String userId) {
        return folderRepository.findByOwnerId(userId);
    }

    /**
     * Get child folders of a folder
     * @param folderId folder ID
     * @param userId user ID
     * @return List of child Folder entities
     */
    public List<Folder> getChildFolders(String folderId, String userId) {
        permissionService.verifyFolderOwnership(folderId, userId);
        return folderRepository.findByParentFolderId(folderId);
    }

    /**
     * Get folder tree starting from a folder
     * Uses a single recursive query to fetch all descendants at once,
     * then builds the tree in memory without additional database calls.
     * @param folderId folder ID
     * @param userId user ID
     * @return FolderTree structure
     */
    public FolderTree getFolderTree(String folderId, String userId) {
        Folder rootFolder = permissionService.getFolderWithCheck(folderId, userId);

        // Fetch all descendants in a single query using recursion
        List<Folder> allDescendants = folderRepository.findAllDescendants(folderId);
        allDescendants.add(rootFolder);

        // Group folders by parent for O(1) lookup
        Map<String, List<Folder>> childrenMap = allDescendants.stream()
            .filter(f -> f.getParentFolderId() != null)
            .collect(Collectors.groupingBy(Folder::getParentFolderId));

        // Fetch page counts for all folders in a single batch query
        List<String> folderIds = allDescendants.stream()
            .map(Folder::getFolderId)
            .collect(Collectors.toList());
        Map<String, Long> pageCountMap = pageService.getPageCountsForFolders(folderIds);

        // Build tree using pre-fetched data
        return buildFolderTree(rootFolder, childrenMap, pageCountMap);
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
            Map<String, Long> pageCountMap
    ) {
        FolderTree tree = new FolderTree();

        tree.setFolderId(folder.getFolderId());
        tree.setName(folder.getName());
        tree.setCreatedAt(folder.getCreatedAt());

        // Get page count from pre-fetched map
        long pageCount = pageCountMap.getOrDefault(folder.getFolderId(), 0L);
        tree.setPageCount(pageCount);

        // Get children from pre-fetched map
        List<Folder> children = childrenMap.getOrDefault(folder.getFolderId(), new ArrayList<>());
        List<FolderTree> childTrees = new ArrayList<>();

        for (Folder child : children) {
            childTrees.add(buildFolderTree(child, childrenMap, pageCountMap));
        }

        tree.setChildren(childTrees);
        tree.setChildCount(childTrees.size());

        return tree;
    }

    /**
     * Get folder trees for all root folders of a user
     * Fetches all folders at once and builds all trees in memory
     * @param userId user ID
     * @return List of FolderTree structures
     */
    public List<FolderTree> getUserFolderTrees(String userId) {
        List<Folder> allFolders = folderRepository.findByOwnerId(userId);

        // Find root folders
        List<Folder> rootFolders = allFolders.stream()
            .filter(f -> f.getParentFolderId() == null)
            .collect(Collectors.toList());

        // Group all folders by parent for O(1) lookup
        Map<String, List<Folder>> childrenMap = allFolders.stream()
            .filter(f -> f.getParentFolderId() != null)
            .collect(Collectors.groupingBy(Folder::getParentFolderId));

        // Fetch page counts for all folders in a single batch query
        List<String> folderIds = allFolders.stream()
            .map(Folder::getFolderId)
            .collect(Collectors.toList());
        Map<String, Long> pageCountMap = pageService.getPageCountsForFolders(folderIds);

        // Build all trees using pre-fetched data
        List<FolderTree> folderTrees = new ArrayList<>();
        for (Folder rootFolder : rootFolders) {
            folderTrees.add(buildFolderTree(rootFolder, childrenMap, pageCountMap));
        }

        return folderTrees;
    }

    /**
     * Move a folder to a new parent folder
     * @param folderId folder ID
     * @param userId user ID
     * @param newParentFolderId new parent folder ID
     * @return Updated Folder entity
     */
    @Transactional
    public Folder moveFolder(String folderId, String userId, String newParentFolderId) {
        Folder folder = getFolder(folderId, userId);

        if (newParentFolderId != null && !newParentFolderId.isEmpty()) {
            Folder newParent = permissionService.getFolderWithCheck(newParentFolderId, userId);

            if (isDescendant(newParent, folderId)) {
                throw new InvalidRequestException("Cannot move folder into itself or its descendants");
            }
        }

        folder.setParentFolderId(newParentFolderId);
        return folderRepository.save(folder);
    }

    /**
     * Check if a folder is a descendant of another folder
     * @param potentialDescendant potential descendant folder
     * @param ancestorId ancestor folder ID
     * @return true if potentialDescendant is a descendant of ancestorId, else false
     */
    private boolean isDescendant(Folder potentialDescendant, String ancestorId) {
        if (potentialDescendant.getFolderId().equals(ancestorId)) {
            return true;
        }

        if (potentialDescendant.getParentFolderId() == null) {
            return false;
        }

        Optional<Folder> parent = folderRepository.findById(potentialDescendant.getParentFolderId());
        return parent.isPresent() && isDescendant(parent.get(), ancestorId);
    }

    /**
     * Get folder path from root to the specified folder
     * @param folderId folder ID
     * @param userId user ID
     * @return List of Folder entities in the path
     */
    public List<Folder> getFolderPath(String folderId, String userId) {
        List<Folder> path = new ArrayList<>();
        Folder current = permissionService.getFolderWithCheck(folderId, userId);

        while (current != null) {
            path.add(0, current);

            if (current.getParentFolderId() != null) {
                current = folderRepository.findById(current.getParentFolderId()).orElse(null);
            } else {
                break;
            }
        }

        return path;
    }
}
