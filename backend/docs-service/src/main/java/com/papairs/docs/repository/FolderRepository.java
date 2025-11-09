package com.papairs.docs.repository;

import com.papairs.docs.model.Folder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FolderRepository extends JpaRepository<Folder, String> {

    /**
     * Finds all folders owned by a specific user
     * @param ownerId The ID of the owner
     * @return A {@link List} of {@link Folder} entities
     */
    List<Folder> findByOwnerId(String ownerId);

    /**
     * Finds all root folders (those without a parent) for a specific user
     * @param ownerId The ID of the owner
     * @return A {@link List} of root-level {@link Folder} entities
     */
    List<Folder> findByOwnerIdAndParentFolderIdIsNull(String ownerId);

    /**
     * Finds all direct child folders within a given parent folder
     * @param parentFolderId The ID of the parent folder
     * @return A {@link List} of child {@link Folder} entities
     */
    List<Folder> findByParentFolderId(String parentFolderId);

    /**
     * Recursively finds all descendant folder IDs for a given parent folder
     * @param folderId The ID of the parent folder
     * @return A {@link List} of all descendant folder IDs (children, grandchildren, etc.)
     */
    @Query(value =
        "WITH RECURSIVE folder_tree (folder_id) AS (" +
            "  SELECT folder_id FROM folder WHERE parent_folder_id = :folderId" +
            "  UNION ALL" +
            "  SELECT f.folder_id FROM folder f" +
            "  INNER JOIN folder_tree ft ON f.parent_folder_id = ft.folder_id" +
            ") SELECT folder_id FROM folder_tree",
        nativeQuery = true)
    List<String> findAllDescendantIds(String folderId);

    /**
     * Checks if a parent folder contains any child folders
     * @param parentFolderId The ID of the parent folder
     * @return {@code true} if at least one child folder exists, {@code false} otherwise
     */
    boolean existsByParentFolderId(String parentFolderId);

    /**
     * Counts the number of direct child folders within a parent folder
     * @param parentFolderId The ID of the parent folder
     * @return The total number of direct child folders
     */
    long countByParentFolderId(String parentFolderId);

    /**
     * Finds the complete ancestor path from a given folder up to the root
     * The list includes the starting folder and is typically ordered from child to root
     * @param folderId The ID of the starting folder
     * @return A {@link List} of {@link Folder} entities representing the path to the root
     */
    @Query(value =
        "WITH RECURSIVE folder_path (folder_id, name, parent_folder_id, owner_id, created_at) AS (" +
            "SELECT folder_id, name, parent_folder_id, owner_id, created_at FROM folder WHERE folder_id = :folderId " +
            "UNION ALL " +
            "SELECT f.folder_id, f.name, f.parent_folder_id, f.owner_id, f.created_at FROM folder f " +
            "INNER JOIN folder_path fp ON f.folder_id = fp.parent_folder_id" +
            ") SELECT * FROM folder_path",
        nativeQuery = true)
    List<Folder> findAncestorPath(String folderId);

    /**
     * Deletes all folders identified by the given list of IDs in a single bulk operation
     * The {@code clearAutomatically = true} option ensures the persistence context is cleared
     * after the query, preventing issues with detached entities
     * @param folderIds A {@link List} of folder IDs to be deleted
     * @see Modifying#clearAutomatically()
     */
    @Modifying(clearAutomatically = true)
    void deleteAllByFolderIdIn(List<String> folderIds);
}
