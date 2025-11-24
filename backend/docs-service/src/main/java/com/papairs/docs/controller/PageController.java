package com.papairs.docs.controller;

import com.papairs.docs.dto.request.CreatePageRequest;
import com.papairs.docs.dto.request.MovePageRequest;
import com.papairs.docs.dto.request.RenamePageRequest;
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
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:8080"}, 
             allowedHeaders = "*", 
             methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class PageController {
    private final PageService pageService;

    public PageController(PageService pageService) {
        this.pageService = pageService;
    }

    /**
     * Get list of pages which the user has access to
     * @param httpRequest HTTP servlet request
     * @return ResponseEntity with list of pages
     */
    @GetMapping("/pages")
    public ResponseEntity<List<Page>> getUserPages(HttpServletRequest httpRequest) {
        List<Page> pages = pageService.getAllAccessiblePages(UserId.extract(httpRequest));
        return ResponseEntity.ok(pages);
    }

    /**
     * Create a new page
     * @param createPageRequest create page request
     * @param request HTTP servlet request
     * @return ResponseEntity with created page
     */
    @PostMapping("/pages")
    public ResponseEntity<Page> createPage(
            @Valid @RequestBody CreatePageRequest createPageRequest,
            HttpServletRequest request
    ) {
        Page page = pageService.createPage(
            createPageRequest.getTitle(),
            UserId.extract(request),
            createPageRequest.getFolderId()
        );
        return ResponseEntity.status(201).body(page);
    }

    /**
     * Get a page by ID
     * @param pageId page ID
     * @param request HTTP servlet request
     * @return ResponseEntity with the page
     */
    @GetMapping("/pages/{pageId}")
    public ResponseEntity<Page> getPage(
            @PathVariable String pageId,
            HttpServletRequest request
    ) {
        Page page = pageService.getPage(pageId, UserId.extract(request));
        return ResponseEntity.ok(page);
    }

    /**
     * Update a page's content
     * @deprecated Use the Websocket implementation for real-time updates {@link com.papairs.docs.ws.DocWebSocketHandler}
     * @param pageId page ID
     * @param updatePageRequest update page request
     * @param request HTTP servlet request
     * @return ResponseEntity with updated page
     */
    @PutMapping("/pages/{pageId}")
    public ResponseEntity<Page> updatePage(
            @PathVariable String pageId,
            @RequestBody UpdatePageRequest updatePageRequest,
            HttpServletRequest request
    ) { 
        Page updated = pageService.updatePage(pageId, UserId.extract(request), updatePageRequest.getContent());
        return ResponseEntity.ok(updated);
    }

    /**
     * Rename a page
     * @param pageId page ID
     * @param renamePageRequest rename page request
     * @param request HTTP servlet request
     * @return ResponseEntity with renamed page
     */
    @PatchMapping("/pages/{pageId}")
    public ResponseEntity<Page> renamePage(
            @PathVariable String pageId,
            @Valid @RequestBody RenamePageRequest renamePageRequest,
            HttpServletRequest request
    ) {
        Page renamed = pageService.renamePage(
            pageId,
            UserId.extract(request),
            renamePageRequest.getNewTitle()
        );
        return ResponseEntity.ok(renamed);
    }

    /**
     * Move a page to a new folder
     * @param pageId page ID
     * @param movePageRequest move page request
     * @param request HTTP servlet request
     * @return ResponseEntity with moved page
     */
    @PatchMapping("/pages/{pageId}/move")
    public ResponseEntity<Page> movePage(
            @PathVariable String pageId,
            @Valid @RequestBody MovePageRequest movePageRequest,
            HttpServletRequest request
    ) {
        Page moved = pageService.movePage(
            pageId,
            movePageRequest.getFolderId(),
            UserId.extract(request)
        );
        return ResponseEntity.ok(moved);
    }

    /**
     * Delete a page by ID
     * @param pageId page ID
     * @param request HTTP servlet request
     * @return ResponseEntity with no content
     */
    @DeleteMapping("/pages/{pageId}")
    public ResponseEntity<Void> deletePage(
            @PathVariable String pageId,
            HttpServletRequest request
    ) {
        pageService.deletePage(pageId, UserId.extract(request));
        return ResponseEntity.noContent().build();
    }
}
