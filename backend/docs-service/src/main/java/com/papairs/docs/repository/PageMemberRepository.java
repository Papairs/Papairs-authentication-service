package com.papairs.docs.repository;

import com.papairs.docs.model.PageMember;
import com.papairs.docs.model.PageMemberId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PageMemberRepository extends JpaRepository<PageMember, PageMemberId> {

    /**
     * Finds all members of a specific page
     * @param pageId The ID of the page
     * @return A list of {@link PageMember} entities
     */
    List<PageMember> findByPageId(String pageId);

    /**
     * Finds all pages a specific user is a member of
     * @param userId The ID of the user
     * @return A list of {@link PageMember} entities
     */
    List<PageMember> findByUserId(String userId);

    /**
     * Finds a specific page membership by page and user IDs
     * @param pageId The ID of the page
     * @param userId The ID of the user
     * @return An {@link Optional} containing the {@link PageMember} if found
     */
    Optional<PageMember> findByPageIdAndUserId(String pageId, String userId);

    /**
     * Checks if a user is a member of a specific page
     * @param pageId The ID of the page
     * @param userId The ID of the user
     * @return {@code true} if the membership exists, {@code false} otherwise
     */
    boolean existsByPageIdAndUserId(String pageId, String userId);

    /**
     * Verifies if a member has at least {@code EDITOR}-level permissions on a page
     * This query checks for {@code EDITOR} role
     * @param pageId The ID of the page
     * @param userId The ID of the user
     * @return {@code true} if the user has edit permissions, {@code false} otherwise
     */
    @Query("SELECT COUNT(pm) > 0 FROM PageMember pm " +
            "WHERE pm.pageId = :pageId AND pm.userId = :userId " +
            "AND pm.role = 'EDITOR'")
    boolean hasEditPermission(String pageId, String userId);

    /**
     * Retrieves all page IDs for a given user
     * Useful for efficiently listing all pages a user has access to
     * @param userId The ID of the user
     * @return A list of page IDs
     */
    @Query("SELECT pm.pageId FROM PageMember pm WHERE pm.userId = :userId")
    List<String> findPageIdsByUserId(String userId);

    /**
     * Deletes a specific page membership
     * @param pageId The ID of the page
     * @param userId The ID of the user
     */
    @Modifying
    void deleteByPageIdAndUserId(String pageId, String userId);

    /**
     * Deletes all memberships associated with a specific page
     * This is a bulk operation and should be used with care
     * @param pageId The ID of the page to remove all members from
     */
    @Modifying
    void deleteByPageId(String pageId);
}
