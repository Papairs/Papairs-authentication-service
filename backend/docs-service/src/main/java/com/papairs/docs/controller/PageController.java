package com.papairs.docs.controller;

import com.papairs.docs.dto.request.CreatePageRequest;
import com.papairs.docs.dto.request.MovePageRequest;
import com.papairs.docs.dto.request.UpdatePageRequest;
import com.papairs.docs.model.Page;
import com.papairs.docs.service.PageService;
import com.papairs.docs.util.UserId;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/docs")
@CrossOrigin(origins = "http://localhost:3000")
public class PageController {
    private final PageService pageService;

    public PageController(PageService pageService) {
        this.pageService = pageService;
    }

    /**
     * Get list of pages for the user
     * @param httpRequest HTTP servlet request
     * @return ResponseEntity with list of pages
     */
    @GetMapping("/pages")
    public ResponseEntity<List<Page>> getPages(HttpServletRequest httpRequest) {
        List<Page> pages = pageService.getUserPages(UserId.extract(httpRequest));
        return ResponseEntity.ok(pages);
    }

    /**
     * Create a new page
     * @param request create page request
     * @param httpRequest HTTP servlet request
     * @return ResponseEntity with created page
     */
    @PostMapping("/pages")
    public ResponseEntity<Page> createPage(
            @Valid @RequestBody CreatePageRequest request,
            HttpServletRequest httpRequest
    ) {
        Page page = pageService.createPage(request.getTitle(), request.getParentFolderId(), UserId.extract(httpRequest));
        return ResponseEntity.status(201).body(page);
    }

    /**
     * Get a page by ID
     * @param pageId page ID
     * @return ResponseEntity with the page
     */
    @GetMapping("/pages/{pageId}")
    public ResponseEntity<Page> getPage(@PathVariable String pageId) {
        return ResponseEntity.ok(pageService.getPage(pageId));
    }

    /**
     * Update a page's content
     * @param pageId page ID
     * @param request update page request
     * @return ResponseEntity with updated page
     */
    @PutMapping("/pages/{pageId}")
    public ResponseEntity<Page> updatePage(
            @PathVariable String pageId,
            @RequestBody UpdatePageRequest request
    ) {
        Page updated = pageService.updatePage(pageId, request.getContent());
        return ResponseEntity.ok(updated);
    }

    /**
     * Rename a page
     * @param pageId page ID
     * @param request rename page request
     * @return ResponseEntity with renamed page
     */
    @PostMapping("/pages/{pageId}")
    public ResponseEntity<Page> renamePage(
            @PathVariable String pageId,
            @RequestBody RenamePageRequest request
    ) {
        Page renamed = pageService.renamePage(pageId, request.getNewTitle());
        return ResponseEntity.ok(renamed);
    }

    /**
     * Move a page to a new folder
     * @param pageId page ID
     * @param request move page request
     * @return
     */
    @PostMapping("/pages/{pageId}/move")
    public ResponseEntity<Page> movePage(
            @PathVariable String pageId,
            @Valid @RequestBody MovePageRequest request
    ) {
        Page moved = pageService.movePage(pageId, request.getNewParentId());
        return ResponseEntity.ok(moved);
    }

    /**
     * Delete a page by ID
     * @param pageId page ID
     * @return ResponseEntity with no content
     */
    @DeleteMapping("/pages/{pageId}")
    public ResponseEntity<Void> deletePage(@PathVariable String pageId) {
        pageService.deletePage(pageId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/pages/{pageId}/members")
    public String getPageMembers() {
        return "List of page members";
    }
}
