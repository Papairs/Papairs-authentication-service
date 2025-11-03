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
     * Delete pages by folder ID
     * @param folderId folder ID
     */
    @Modifying
    @Query("DELETE FROM Page p WHERE p.folderId = :folderId")
    void deleteByFolderId(String folderId);
}
