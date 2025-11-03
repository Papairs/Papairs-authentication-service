package com.papairs.docs.repository;

import com.papairs.docs.model.Folder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FolderRepository extends JpaRepository<Folder, String> {
    /**
     * Find folders by owner ID
     * @param ownerId owner's user ID
     * @return List of Folder entities
     */
    List<Folder> findByOwnerId(String ownerId);

    /**
     * Find root folders by owner ID
     * @param ownerId owner's user ID
     * @return List of root Folder entities
     */
    List<Folder> findByOwnerIdAndParentFolderIdIsNull(String ownerId);

    /**
     * Find folders by parent folder ID
     * @param parentFolderId parent folder ID
     * @return List of Folder entities
     */
    List<Folder> findByParentFolderId(String parentFolderId);

    /**
     * Find all descendant folders of a given folder using recursion
     * @param folderId folder ID
     * @return List of descendant Folder entities
     */
    @Query(value =
            "WITH RECURSIVE folder_tree AS (" +
                    "SELECT * FROM folder " +
                    "WHERE parent_folder_id = :folderId " +
                    "UNION ALL " +
                    "SELECT f.* FROM folder f " +
                    "INNER JOIN folder_tree ft ON f.parent_folder_id = ft.folder_id) " +
                    "SELECT * FROM folder_tree",
            nativeQuery = true)
    List<Folder> findAllDescendants(String folderId);

    /**
     * Check if any folders exist in the parent folder
     * @param parentFolderId parent folder ID
     * @return true if folders exist, else false
     */
    boolean existsByParentFolderId(String parentFolderId);

    /**
     * Count folders in the parent folder
     * @param parentFolderId parent folder ID
     * @return number of folders in the parent folder
     */
    long countByParentFolderId(String parentFolderId);
}
