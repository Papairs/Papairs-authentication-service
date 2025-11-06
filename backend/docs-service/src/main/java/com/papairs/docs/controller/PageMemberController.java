package com.papairs.docs.controller;

import com.papairs.docs.dto.request.AddPageMemberRequest;
import com.papairs.docs.dto.request.UpdatePageMemberRequest;
import com.papairs.docs.model.PageMember;
import com.papairs.docs.service.PageMemberService;
import com.papairs.docs.util.UserId;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/docs")
@CrossOrigin(origins = "http://localhost:3000")
public class PageMemberController {
    private final PageMemberService pageMemberService;

    public PageMemberController(PageMemberService pageMemberService) {
        this.pageMemberService = pageMemberService;
    }

    /**
     * Retrieves a list of all members for a specific page
     * The requesting user must have access to the page to view its members
     * @param pageId The ID of the page
     * @param request The incoming HTTP request which is used to identify the requesting user
     * @return A {@link ResponseEntity} containing a list of {@link PageMember} objects
     */
    @GetMapping("/pages/{pageId}/members")
    public ResponseEntity<List<PageMember>> getPageMembers(
            @PathVariable String pageId,
            HttpServletRequest request
    ) {
        List<PageMember> members = pageMemberService.getPageMembers(pageId, UserId.extract(request));
        return ResponseEntity.ok(members);
    }

    /**
     * Adds a new member to a page
     * This action is restricted to the page owner or members with an editor role
     * @param pageId The ID of the page to add the member to.
     * @param addPageMemberRequest The request body containing the target user's ID and role
     * @param request The incoming HTTP request
     * @return A {@link ResponseEntity} with status 201 (Created) and the new {@link PageMember}.
     */
    @PostMapping("/pages/{pageId}/members")
    public ResponseEntity<PageMember> addMember(
            @PathVariable String pageId,
            @Valid @RequestBody AddPageMemberRequest addPageMemberRequest,
            HttpServletRequest request
    ) {
        PageMember newMember = pageMemberService.addMember(
            pageId,
            UserId.extract(request),
            addPageMemberRequest.getUserId(),
            addPageMemberRequest.getRole()
        );

        return ResponseEntity.status(201).body(newMember);
    }

    /**
     * Updates the role of an existing member on a page
     * This action is restricted to the page owner or members with an editor role
     * @param pageId The ID of the page
     * @param userId The ID of the member whose role is being updated
     * @param updatePageMemberRequest The request body containing the new role
     * @param request The incoming HTTP request
     * @return A {@link ResponseEntity} containing the updated {@link PageMember}
     */
    @PatchMapping("/pages/{pageId}/members/{userId}")
    public ResponseEntity<PageMember> updateMemberRole(
            @PathVariable String pageId,
            @PathVariable String userId,
            @Valid @RequestBody UpdatePageMemberRequest updatePageMemberRequest,
            HttpServletRequest request
            ) {
        PageMember updatedMember = pageMemberService.updateMemberRole(
            pageId,
            UserId.extract(request),
            userId,
            updatePageMemberRequest.getRole()
        );

        return ResponseEntity.ok(updatedMember);
    }

    /**
     * Removes a member from a page
     * This action is restricted to the page owner or members with an editor role
     * @param pageId  The ID of the page
     * @param userId  The ID of the member to remove
     * @param request The incoming HTTP request
     * @return A {@link ResponseEntity} with status 204 (No Content)
     */
    @DeleteMapping("/pages/{pageId}/members/{userId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable String pageId,
            @PathVariable String userId,
            HttpServletRequest request
    ) {
        pageMemberService.removeMember(
            pageId,
            UserId.extract(request),
            userId
        );

        return ResponseEntity.noContent().build();
    }

    /**
     * Allows a user to leave a page by removing their own membership
     * The page owner cannot leave a page and must delete the page instead
     * @param pageId  The ID of the page to leave
     * @param request The incoming HTTP request
     * @return A {@link ResponseEntity} with status 204 (No Content)
     */
    @DeleteMapping("/pages/{pageId}/leave")
    public ResponseEntity<Void> leavePage(
            @PathVariable String pageId,
            HttpServletRequest request
    ) {
        pageMemberService.leavePage(pageId, UserId.extract(request));

        return ResponseEntity.noContent().build();
    }
}
