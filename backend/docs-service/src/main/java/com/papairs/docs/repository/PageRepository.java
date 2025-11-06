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
     * Finds all pages owned by a specific user
     * @param ownerId The ID of the owner
     * @return A {@link List} of {@link Page} entities owned by the user
     */
    List<Page> findByOwnerId(String ownerId);

    /**
     * Finds all pages located within a specific folder
     * @param folderId The ID of the folder
     * @return A {@link List} of {@link Page} entities within the folder.
     */
    List<Page> findByFolderId(String folderId);

    /**
     * Checks if any pages exist within a specific folder.
     * This is more performant than fetching the list of pages and checking if it's empty
     * @param folderId The ID of the folder
     * @return {@code true} if at least one page exists, {@code false} otherwise
     */
    boolean existsByFolderId(String folderId);

    /**
     * Counts the total number of pages within a specific folder
     * @param folderId The ID of the folder
     * @return The number of pages in the folder
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
     * Finds all pages that are shared with a specific user via a {@code PageMember} association
     * This does not include pages owned by the user unless they are also explicitly a member
     * @param userId The ID of the user who is a member of the pages
     * @return A {@link List} of {@link Page} entities shared with the user
     */
    @Query("SELECT p FROM Page p " +
            "JOIN PageMember pm ON p.pageId = pm.pageId " +
            "WHERE pm.userId = :userId")
    List<Page> findPagesSharedWithUser(String userId);

    /**
     * Deletes all pages located within a specific folder
     * This is a bulk operation and may have performance implications on large datasets
     * @param folderId The ID of the folder whose pages should be deleted
     */
    @Modifying
    @Query("DELETE FROM Page p WHERE p.folderId = :folderId")
    void deleteAllByFolderId(String folderId);

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
