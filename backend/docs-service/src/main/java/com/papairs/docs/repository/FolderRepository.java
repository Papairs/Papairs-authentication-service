package com.papairs.docs.repository;

import com.papairs.docs.model.Folder;
import org.springframework.data.jpa.repository.JpaRepository;
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
