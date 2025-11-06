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

    @GetMapping("/pages/{pageId}/members")
    public ResponseEntity<List<PageMember>> getPageMembers(
            @PathVariable String pageId,
            HttpServletRequest request
    ) {
        List<PageMember> members = pageMemberService.getPageMembers(pageId, UserId.extract(request));
        return ResponseEntity.ok(members);
    }

    @PostMapping("/pages/{pageId}/members")
    public ResponseEntity<PageMember> addMember(
            @PathVariable String pageId,
            @Valid @RequestBody AddPageMemberRequest addPageMemberRequest,
            HttpServletRequest request
    ) {
        PageMember member = pageMemberService.addMember(
            pageId,
            UserId.extract(request),
            addPageMemberRequest.getUserId(),
            addPageMemberRequest.getRole()
        );

        return ResponseEntity.status(201).body(member);
    }

    @PatchMapping("/pages/{pageId}/members/{userId}")
    public ResponseEntity<PageMember> updateMemberRole(
            @PathVariable String pageId,
            @PathVariable String userId,
            @Valid @RequestBody UpdatePageMemberRequest updatePageMemberRequest,
            HttpServletRequest request
            ) {
        PageMember updated = pageMemberService.updateMemberRole(
            pageId,
            UserId.extract(request),
            userId,
            updatePageMemberRequest.getRole()
        );

        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/pages/{pageId}/members/{userId}")
    public ResponseEntity<Void> deleteMember(
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

    @PostMapping("/pages/{pageId}/leave")
    public ResponseEntity<Void> leavePage(
            @PathVariable String pageId,
            HttpServletRequest request
    ) {
        pageMemberService.leavePage(pageId, UserId.extract(request));

        return ResponseEntity.noContent().build();
    }
}
