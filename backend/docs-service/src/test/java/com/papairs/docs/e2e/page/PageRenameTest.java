package com.papairs.docs.e2e.page;

import com.papairs.docs.e2e.AbstractE2ETest;
import com.papairs.docs.model.enums.MemberRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Page Rename")
public class PageRenameTest extends AbstractE2ETest {
    @Test
    @DisplayName("Should allow owner to rename their page")
    public void ownerCanRenamePage() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Original Title");

        String renameRequest = """
            {
                "newTitle": "Updated Title"
            }
            """;

        mockMvc.perform(patch("/api/docs/pages/" + pageId)
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(renameRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageId").value(pageId))
                .andExpect(jsonPath("$.title").value("Updated Title"));

        fixtures.verifyPageTitle(pageId, "Updated Title", TEST_USER_1_ID);
    }

    @Test
    @DisplayName("Should allow EDITOR member to rename page")
    public void editorCanRenamePage() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Original Title");
        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.EDITOR);

        String renameRequest = """
            {
                "newTitle": "Editor Updated Title"
            }
            """;

        mockMvc.perform(patch("/api/docs/pages/" + pageId)
                        .header(USER_ID_HEADER, TEST_USER_2_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(renameRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Editor Updated Title"));

        fixtures.verifyPageTitle(pageId, "Editor Updated Title", TEST_USER_1_ID);
    }

    @Test
    @DisplayName("Should deny rename by VIEWER member")
    public void viewerCannotRenamePage() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Original Title");
        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.VIEWER);

        String renameRequest = """
            {
                "newTitle": "Viewer Attempted Update"
            }
            """;

        mockMvc.perform(patch("/api/docs/pages/" + pageId)
                        .header(USER_ID_HEADER, TEST_USER_2_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(renameRequest))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(containsString("permission")));

        fixtures.verifyPageTitle(pageId, "Original Title", TEST_USER_1_ID);
    }

    @Test
    @DisplayName("Should deny rename by non-member")
    public void nonMemberCannotRenamePage() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Original Title");

        String renameRequest = """
            {
                "newTitle": "Unauthorized Update"
            }
            """;

        mockMvc.perform(patch("/api/docs/pages/" + pageId)
                        .header(USER_ID_HEADER, TEST_USER_2_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(renameRequest))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(containsString("permission")));

        fixtures.verifyPageTitle(pageId, "Original Title", TEST_USER_1_ID);
    }

    @Test
    @DisplayName("Should rename page with trimmed title")
    public void renamePageWithTrimmedTitle() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Original Title");

        String renameRequest = """
            {
                "newTitle": "  Trimmed Title  "
            }
            """;

        mockMvc.perform(patch("/api/docs/pages/" + pageId)
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(renameRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Trimmed Title"));

        fixtures.verifyPageTitle(pageId, "Trimmed Title", TEST_USER_1_ID);
    }

    @Test
    @DisplayName("Should reject rename with blank title")
    public void cannotRenameWithBlankTitle() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Original Title");

        String renameRequest = """
            {
                "newTitle": ""
            }
            """;

        mockMvc.perform(patch("/api/docs/pages/" + pageId)
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(renameRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());

        fixtures.verifyPageTitle(pageId, "Original Title", TEST_USER_1_ID);
    }

    @Test
    @DisplayName("Should reject rename with whitespace-only title")
    public void cannotRenameWithWhitespaceTitle() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Original Title");

        String renameRequest = """
            {
                "newTitle": "     "
            }
            """;

        mockMvc.perform(patch("/api/docs/pages/" + pageId)
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(renameRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());

        fixtures.verifyPageTitle(pageId, "Original Title", TEST_USER_1_ID);
    }

    @Test
    @DisplayName("Should reject rename with whitespace-only title")
    public void cannotRenameWithNullTitle() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Original Title");

        String renameRequest = """
            {
                "newTitle": null
            }
            """;

        mockMvc.perform(patch("/api/docs/pages/" + pageId)
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(renameRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());

        fixtures.verifyPageTitle(pageId, "Original Title", TEST_USER_1_ID);
    }
}
