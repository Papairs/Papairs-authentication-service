package com.papairs.docs.repository;

import com.papairs.docs.model.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PageRepository extends JpaRepository<Page, String> {

    /**
     * Find pages by owner ID
     * @param ownerId owner's user ID
     * @return List of pages owned by the user
     */
    List<Page> findByOwnerId(String ownerId);

    /**
     * Find pages by folder ID
     * @param folderId folder ID
     * @return List of pages in the folder
     */
    List<Page> findByFolderId(String folderId);

    /**
     * Check if any pages exist in the folder
     * @param folderId folder ID
     * @return true if pages exist, else false
     */
    boolean existsByFolderId(String folderId);

    /**
     * Count pages in the folder
     * @param folderId folder ID
     * @return number of pages in the folder
     */
    long countByFolderId(String folderId);

    /**
     * Get page counts for multiple folders in a single query
     * Returns a list of Object arrays where [0] is folderId and [1] is count
     * @param folderIds list of folder IDs
     * @return List of Object arrays [folderId, count]
     */
    @Query("SELECT p.folderId, COUNT(p) FROM Page p WHERE p.folderId IN :folderIds GROUP BY p.folderId")
    List<Object[]> countByFolderIdIn(List<String> folderIds);

    /**
     * Delete pages by folder ID
     * @param folderId folder ID
     * Finds all pages that are shared with a specific user via a {@code PageMember} association
     * This does not include pages owned by the user unless they are also explicitly a member
     * @param userId The ID of the user who is a member of the pages
     * @return A {@link List} of {@link Page} entities shared with the user
     */
    @Query("SELECT p FROM Page p " +
            "JOIN PageMember pm ON p.pageId = pm.pageId " +
            "WHERE pm.userId = :userId")
    List<Page> findPagesSharedWithUser(String userId);
     */
    @Modifying
    @Query("DELETE FROM Page p WHERE p.folderId = :folderId")
    void deleteByFolderId(String folderId);

    /**
     * Deletes all pages within a given list of folder IDs in a single bulk operation
     * The {@code clearAutomatically = true} option ensures the persistence context is cleared
     * after the query which preventing issues with detached entities.
     * @param folderIds A {@link List} of folder IDs to delete pages from.
     * @see Modifying#clearAutomatically()
     */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Page p WHERE p.folderId IN :folderIds")
    void deleteAllByFolderIdIn(List<String> folderIds);
}
