package com.papairs.docs.e2e.folder;

import com.papairs.docs.e2e.AbstractE2ETest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Folder Hierarchy")
public class FolderHierarchyTest extends AbstractE2ETest {

    @Test
    @DisplayName("Should create root-level folder")
    public void canCreateRootFolder() throws Exception {
        String requestBody = """
            {
                "name": "Root Folder"
            }
            """;

        mockMvc.perform(post("/api/docs/folders")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.folderId").exists())
                .andExpect(jsonPath("$.name").value("Root Folder"))
                .andExpect(jsonPath("$.parentFolderId").isEmpty())
                .andExpect(jsonPath("$.ownerId").value(TEST_USER_1_ID));
    }

    @Test
    @DisplayName("Should create nested folder inside parent")
    public void canCreateNestedFolder() throws Exception {
        String parentId = fixtures.createFolderAsUser(TEST_USER_1_ID, "Parent");

        String requestBody = """
            {
                "name": "Child Folder",
                "parentFolderId": "%s"
            }
            """.formatted(parentId);

        mockMvc.perform(post("/api/docs/folders")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Child Folder"))
                .andExpect(jsonPath("$.parentFolderId").value(parentId));
    }

    @Test
    @DisplayName("Should create deeply nested folder hierarchy")
    public void canCreateDeeplyNestedFolders() throws Exception {
        String level1 = fixtures.createFolderAsUser(TEST_USER_1_ID, "Level 1");
        String level2 = fixtures.createFolderAsUser(TEST_USER_1_ID, "Level 2", level1);
        String level3 = fixtures.createFolderAsUser(TEST_USER_1_ID, "Level 3", level2);
        String level4 = fixtures.createFolderAsUser(TEST_USER_1_ID, "Level 4", level3);
        String level5 = fixtures.createFolderAsUser(TEST_USER_1_ID, "Level 5", level4);

        mockMvc.perform(get("/api/docs/folders/" + level5)
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parentFolderId").value(level4));
    }

    @Test
    @DisplayName("Should retrieve folder tree with correct structure")
    public void folderTreeHasCorrectStructure() throws Exception {
        String rootId = fixtures.createFolderAsUser(TEST_USER_1_ID, "Root");
        String child1Id = fixtures.createFolderAsUser(TEST_USER_1_ID, "Child 1", rootId);
        String child2Id = fixtures.createFolderAsUser(TEST_USER_1_ID, "Child 2", rootId);
        String grandchild1Id = fixtures.createFolderAsUser(TEST_USER_1_ID, "Grandchild 1", child1Id);
        String grandchild2Id = fixtures.createFolderAsUser(TEST_USER_1_ID, "Grandchild 2", child1Id);

        mockMvc.perform(get("/api/docs/folders/" + rootId + "/tree")
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.folderId").value(rootId))
                .andExpect(jsonPath("$.name").value("Root"))
                .andExpect(jsonPath("$.childCount").value(2))
                .andExpect(jsonPath("$.children[?(@.folderId == '%s')]".formatted(child1Id)).exists())
                .andExpect(jsonPath("$.children[?(@.folderId == '%s')]".formatted(child2Id)).exists())
                .andExpect(jsonPath("$.children[?(@.folderId == '%s')].childCount".formatted(child1Id)).value(2))
                .andExpect(jsonPath("$.children[?(@.folderId == '%s')].children[?(@.folderId == '%s')]".formatted(child1Id, grandchild1Id)).exists())
                .andExpect(jsonPath("$.children[?(@.folderId == '%s')].children[?(@.folderId == '%s')]".formatted(child1Id, grandchild2Id)).exists());
    }

    @Test
    @DisplayName("Should retrieve folder path from root to target")
    public void folderPathShowsCompleteHierarchy() throws Exception {
        String level1 = fixtures.createFolderAsUser(TEST_USER_1_ID, "Level 1");
        String level2 = fixtures.createFolderAsUser(TEST_USER_1_ID, "Level 2", level1);
        String level3 = fixtures.createFolderAsUser(TEST_USER_1_ID, "Level 3", level2);

        mockMvc.perform(get("/api/docs/folders/" + level3 + "/path")
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].folderId").value(level1))
                .andExpect(jsonPath("$[1].folderId").value(level2))
                .andExpect(jsonPath("$[2].folderId").value(level3));
    }

    @Test
    @DisplayName("Should retrieve root folders only")
    public void getRootFoldersExcludesNestedFolders() throws Exception {
        String root1 = fixtures.createFolderAsUser(TEST_USER_1_ID, "Root 1");
        String root2 = fixtures.createFolderAsUser(TEST_USER_1_ID, "Root 2");
        String nestedFolder = fixtures.createFolderAsUser(TEST_USER_1_ID, "Nested", root1);

        mockMvc.perform(get("/api/docs/folders/roots")
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.folderId == '%s')]".formatted(root1)).exists())
                .andExpect(jsonPath("$[?(@.folderId == '%s')]".formatted(root2)).exists())
                .andExpect(jsonPath("$[?(@.folderId == '%s')]".formatted(nestedFolder)).doesNotExist());
    }

    @Test
    @DisplayName("Should retrieve all user folder trees")
    public void getUserFolderTreesShowsAllRootTrees() throws Exception {
        String root1 = fixtures.createFolderAsUser(TEST_USER_1_ID, "Root 1");
        String child1 = fixtures.createFolderAsUser(TEST_USER_1_ID, "Child 1-1", root1);

        String root2 = fixtures.createFolderAsUser(TEST_USER_1_ID, "Root 2");
        String child2 = fixtures.createFolderAsUser(TEST_USER_1_ID, "Child 2-1", root2);

        mockMvc.perform(get("/api/docs/folders/trees")
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.folderId == '%s')]".formatted(root1)).exists())
                .andExpect(jsonPath("$[?(@.folderId == '%s')]".formatted(root2)).exists())
                .andExpect(jsonPath("$[?(@.folderId == '%s')].children[0].folderId".formatted(root1)).value(child1))
                .andExpect(jsonPath("$[?(@.folderId == '%s')].children[0].folderId".formatted(root2)).value(child2));
    }

    @Test
    @DisplayName("Should allow owner to move folder to different parent")
    public void ownerCanMoveFolderToNewParent() throws Exception {
        String parent1 = fixtures.createFolderAsUser(TEST_USER_1_ID, "Parent 1");
        String parent2 = fixtures.createFolderAsUser(TEST_USER_1_ID, "Parent 2");
        String childFolder = fixtures.createFolderAsUser(TEST_USER_1_ID, "Child", parent1);

        String moveRequest = """
            {
                "parentFolderId": "%s"
            }
            """.formatted(parent2);

        mockMvc.perform(patch("/api/docs/folders/" + childFolder + "/move")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(moveRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parentFolderId").value(parent2));

        fixtures.verifyFolderParent(childFolder, parent2, TEST_USER_1_ID);
    }

    @Test
    @DisplayName("Should allow owner to move folder to root level")
    public void ownerCanMoveFolderToRoot() throws Exception {
        String parent = fixtures.createFolderAsUser(TEST_USER_1_ID, "Parent");
        String childFolder = fixtures.createFolderAsUser(TEST_USER_1_ID, "Child", parent);

        String moveRequest = """
            {
                "parentFolderId": null
            }
            """;

        mockMvc.perform(patch("/api/docs/folders/" + childFolder + "/move")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(moveRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parentFolderId").isEmpty());

        fixtures.verifyFolderIsRoot(childFolder, TEST_USER_1_ID);
    }

    @Test
    @DisplayName("Should deny non-owner from moving folder")
    public void nonOwnerCannotMoveFolder() throws Exception {
        String parent = fixtures.createFolderAsUser(TEST_USER_1_ID, "Parent");
        String childFolder = fixtures.createFolderAsUser(TEST_USER_1_ID, "Child", parent);

        String moveRequest = """
            {
                "parentFolderId": null
            }
            """;

        mockMvc.perform(patch("/api/docs/folders/" + childFolder + "/move")
                        .header(USER_ID_HEADER, TEST_USER_2_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(moveRequest))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should deny moving folder into itself")
    public void cannotMoveFolderIntoItself() throws Exception {
        String folderId = fixtures.createFolderAsUser(TEST_USER_1_ID, "Self Reference");

        String moveRequest = """
            {
                "parentFolderId": "%s"
            }
            """.formatted(folderId);

        mockMvc.perform(patch("/api/docs/folders/" + folderId + "/move")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(moveRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("itself")));

        fixtures.verifyFolderIsRoot(folderId, TEST_USER_1_ID);
    }

    @Test
    @DisplayName("Should deny moving folder into its own descendant")
    public void cannotMoveFolderIntoDescendant() throws Exception {
        String grandparent = fixtures.createFolderAsUser(TEST_USER_1_ID, "Grandparent");
        String parent = fixtures.createFolderAsUser(TEST_USER_1_ID, "Parent", grandparent);
        String child = fixtures.createFolderAsUser(TEST_USER_1_ID, "Child", parent);

        String moveRequest = """
            {
                "parentFolderId": "%s"
            }
            """.formatted(child);

        mockMvc.perform(patch("/api/docs/folders/" + grandparent + "/move")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(moveRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("descendant")));

        fixtures.verifyFolderIsRoot(grandparent, TEST_USER_1_ID);
    }

    @Test
    @DisplayName("Should return 404 when moving non-existent folder")
    public void movingNonExistentFolderReturns404() throws Exception {
        String nonExistentFolderId = "non-existent-folder-id";

        String moveRequest = """
            {
                "parentFolderId": null
            }
            """;

        mockMvc.perform(patch("/api/docs/folders/" + nonExistentFolderId + "/move")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(moveRequest))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 404 when moving to non-existent parent")
    public void movingToNonExistentParentReturns404() throws Exception {
        String folderId = fixtures.createFolderAsUser(TEST_USER_1_ID, "Folder");
        String nonExistentParentId = "non-existent-parent-id";

        String moveRequest = """
            {
                "parentFolderId": "%s"
            }
            """.formatted(nonExistentParentId);

        mockMvc.perform(patch("/api/docs/folders/" + folderId + "/move")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(moveRequest))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("EDGE: Empty string parentFolderId moves to root")
    public void emptyStringParentMovesToRoot() throws Exception {
        String parent = fixtures.createFolderAsUser(TEST_USER_1_ID, "Parent");
        String child = fixtures.createFolderAsUser(TEST_USER_1_ID, "Child", parent);

        String moveRequest = """
            {
                "parentFolderId": ""
            }
            """;

        mockMvc.perform(patch("/api/docs/folders/" + child + "/move")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(moveRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parentFolderId").isEmpty());

        fixtures.verifyFolderIsRoot(child, TEST_USER_1_ID);
    }

    @Test
    @DisplayName("EDGE: Whitespace parentFolderId moves to root")
    public void whitespaceParentMovesToRoot() throws Exception {
        String parent = fixtures.createFolderAsUser(TEST_USER_1_ID, "Parent");
        String child = fixtures.createFolderAsUser(TEST_USER_1_ID, "Child", parent);

        String moveRequest = """
            {
                "parentFolderId": "   "
            }
            """;

        mockMvc.perform(patch("/api/docs/folders/" + child + "/move")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(moveRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parentFolderId").isEmpty());

        fixtures.verifyFolderIsRoot(child, TEST_USER_1_ID);
    }

    @Test
    @DisplayName("EDGE: Moving folder to same parent succeeds idempotently")
    public void movingToSameParentSucceeds() throws Exception {
        String parent = fixtures.createFolderAsUser(TEST_USER_1_ID, "Parent");
        String child = fixtures.createFolderAsUser(TEST_USER_1_ID, "Child", parent);

        String moveRequest = """
            {
                "parentFolderId": "%s"
            }
            """.formatted(parent);

        mockMvc.perform(patch("/api/docs/folders/" + child + "/move")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(moveRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parentFolderId").value(parent));
    }

    @Test
    @DisplayName("EDGE: Moving root folder to root succeeds idempotently")
    public void movingRootToRootSucceeds() throws Exception {
        String rootFolder = fixtures.createFolderAsUser(TEST_USER_1_ID, "Root");

        String moveRequest = """
            {
                "parentFolderId": null
            }
            """;

        mockMvc.perform(patch("/api/docs/folders/" + rootFolder + "/move")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(moveRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parentFolderId").isEmpty());
    }

    @Test
    @DisplayName("EDGE: Moving folder preserves subfolder hierarchy")
    public void movingFolderPreservesSubfolders() throws Exception {
        String oldParent = fixtures.createFolderAsUser(TEST_USER_1_ID, "Old Parent");
        String newParent = fixtures.createFolderAsUser(TEST_USER_1_ID, "New Parent");
        String folderToMove = fixtures.createFolderAsUser(TEST_USER_1_ID, "Folder to Move", oldParent);
        String subfolder = fixtures.createFolderAsUser(TEST_USER_1_ID, "Subfolder", folderToMove);
        String deepSubfolder = fixtures.createFolderAsUser(TEST_USER_1_ID, "Deep Subfolder", subfolder);

        String moveRequest = """
            {
                "parentFolderId": "%s"
            }
            """.formatted(newParent);

        mockMvc.perform(patch("/api/docs/folders/" + folderToMove + "/move")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(moveRequest))
                .andExpect(status().isOk());

        fixtures.verifyFolderParent(folderToMove, newParent, TEST_USER_1_ID);
        fixtures.verifyFolderParent(subfolder, folderToMove, TEST_USER_1_ID);
        fixtures.verifyFolderParent(deepSubfolder, subfolder, TEST_USER_1_ID);
    }

    @Test
    @DisplayName("EDGE: Moving folder preserves pages inside")
    public void movingFolderPreservesPages() throws Exception {
        String oldParent = fixtures.createFolderAsUser(TEST_USER_1_ID, "Old Parent");
        String newParent = fixtures.createFolderAsUser(TEST_USER_1_ID, "New Parent");
        String folderToMove = fixtures.createFolderAsUser(TEST_USER_1_ID, "Folder", oldParent);

        String page1 = fixtures.createPageInFolder(TEST_USER_1_ID, "Page 1", folderToMove);
        String page2 = fixtures.createPageInFolder(TEST_USER_1_ID, "Page 2", folderToMove);

        String moveRequest = """
            {
                "parentFolderId": "%s"
            }
            """.formatted(newParent);

        mockMvc.perform(patch("/api/docs/folders/" + folderToMove + "/move")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(moveRequest))
                .andExpect(status().isOk());

        fixtures.verifyPageIsInFolder(page1, folderToMove, TEST_USER_1_ID);
        fixtures.verifyPageIsInFolder(page2, folderToMove, TEST_USER_1_ID);
    }

    @Test
    @DisplayName("EDGE: Folder tree shows correct page counts at all levels")
    public void folderTreeShowsCorrectPageCounts() throws Exception {
        String root = fixtures.createFolderAsUser(TEST_USER_1_ID, "Root");
        String child1 = fixtures.createFolderAsUser(TEST_USER_1_ID, "Child 1", root);
        String child2 = fixtures.createFolderAsUser(TEST_USER_1_ID, "Child 2", root);
        String grandchild = fixtures.createFolderAsUser(TEST_USER_1_ID, "Grandchild", child1);

        fixtures.createPageInFolder(TEST_USER_1_ID, "Page in Root", root);
        fixtures.createPageInFolder(TEST_USER_1_ID, "Page 1 in Child 1", child1);
        fixtures.createPageInFolder(TEST_USER_1_ID, "Page 2 in Child 1", child1);
        fixtures.createPageInFolder(TEST_USER_1_ID, "Page in Grandchild", grandchild);

        mockMvc.perform(get("/api/docs/folders/" + root + "/tree")
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageCount").value(1))
                .andExpect(jsonPath("$.children[?(@.folderId == '%s')].pageCount".formatted(child1)).value(2))
                .andExpect(jsonPath("$.children[?(@.folderId == '%s')].pageCount".formatted(child2)).value(0))
                .andExpect(jsonPath("$.children[?(@.folderId == '%s')].children[0].pageCount".formatted(child1)).value(1));
    }

    @Test
    @DisplayName("EDGE: Folder path for root folder returns single element")
    public void folderPathForRootReturnsSingleElement() throws Exception {
        String rootFolder = fixtures.createFolderAsUser(TEST_USER_1_ID, "Root");

        mockMvc.perform(get("/api/docs/folders/" + rootFolder + "/path")
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].folderId").value(rootFolder));
    }

    @Test
    @DisplayName("EDGE: Can create multiple folders with same name in different parents")
    public void canCreateDuplicateNamesInDifferentParents() throws Exception {
        String parent1 = fixtures.createFolderAsUser(TEST_USER_1_ID, "Parent 1");
        String parent2 = fixtures.createFolderAsUser(TEST_USER_1_ID, "Parent 2");

        String child1 = fixtures.createFolderAsUser(TEST_USER_1_ID, "Duplicate Name", parent1);
        String child2 = fixtures.createFolderAsUser(TEST_USER_1_ID, "Duplicate Name", parent2);

        fixtures.verifyFolderParent(child1, parent1, TEST_USER_1_ID);
        fixtures.verifyFolderParent(child2, parent2, TEST_USER_1_ID);
        assert !child1.equals(child2) : "Folders should have different IDs";
    }

    @Test
    @DisplayName("EDGE: Can create multiple folders with same name at root level")
    public void canCreateDuplicateNamesAtRoot() throws Exception {
        String root1 = fixtures.createFolderAsUser(TEST_USER_1_ID, "Duplicate Root");
        String root2 = fixtures.createFolderAsUser(TEST_USER_1_ID, "Duplicate Root");

        fixtures.verifyFolderIsRoot(root1, TEST_USER_1_ID);
        fixtures.verifyFolderIsRoot(root2, TEST_USER_1_ID);
        assert !root1.equals(root2) : "Folders should have different IDs";
    }

    @Test
    @DisplayName("EDGE: Moving folder updates child count in old and new parents")
    public void movingFolderUpdatesChildCounts() throws Exception {
        String oldParent = fixtures.createFolderAsUser(TEST_USER_1_ID, "Old Parent");
        String newParent = fixtures.createFolderAsUser(TEST_USER_1_ID, "New Parent");
        String child1 = fixtures.createFolderAsUser(TEST_USER_1_ID, "Child 1", oldParent);
        fixtures.createFolderAsUser(TEST_USER_1_ID, "Child 2", oldParent);

        fixtures.verifyFolderChildCount(oldParent, 2, TEST_USER_1_ID);
        fixtures.verifyFolderChildCount(newParent, 0, TEST_USER_1_ID);

        String moveRequest = """
            {
                "parentFolderId": "%s"
            }
            """.formatted(newParent);

        mockMvc.perform(patch("/api/docs/folders/" + child1 + "/move")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(moveRequest))
                .andExpect(status().isOk());

        fixtures.verifyFolderChildCount(oldParent, 1, TEST_USER_1_ID);
        fixtures.verifyFolderChildCount(newParent, 1, TEST_USER_1_ID);
    }

    @Test
    @DisplayName("EDGE: Folder tree for deeply nested folder shows complete hierarchy")
    public void deeplyNestedFolderTreeIsComplete() throws Exception {
        String level1 = fixtures.createFolderAsUser(TEST_USER_1_ID, "Level 1");
        String level2 = fixtures.createFolderAsUser(TEST_USER_1_ID, "Level 2", level1);
        String level3 = fixtures.createFolderAsUser(TEST_USER_1_ID, "Level 3", level2);
        String level4 = fixtures.createFolderAsUser(TEST_USER_1_ID, "Level 4", level3);

        mockMvc.perform(get("/api/docs/folders/" + level1 + "/tree")
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.folderId").value(level1))
                .andExpect(jsonPath("$.children[0].folderId").value(level2))
                .andExpect(jsonPath("$.children[0].children[0].folderId").value(level3))
                .andExpect(jsonPath("$.children[0].children[0].children[0].folderId").value(level4));
    }

    @Test
    @DisplayName("EDGE: Get child folders returns only direct children")
    public void getChildFoldersReturnsOnlyDirectChildren() throws Exception {
        String parent = fixtures.createFolderAsUser(TEST_USER_1_ID, "Parent");
        String child1 = fixtures.createFolderAsUser(TEST_USER_1_ID, "Child 1", parent);
        String child2 = fixtures.createFolderAsUser(TEST_USER_1_ID, "Child 2", parent);
        String grandchild = fixtures.createFolderAsUser(TEST_USER_1_ID, "Grandchild", child1);

        mockMvc.perform(get("/api/docs/folders/" + parent + "/children")
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.folderId == '%s')]".formatted(child1)).exists())
                .andExpect(jsonPath("$[?(@.folderId == '%s')]".formatted(child2)).exists())
                .andExpect(jsonPath("$[?(@.folderId == '%s')]".formatted(grandchild)).doesNotExist());
    }

    @Test
    @DisplayName("EDGE: Cannot move folder to parent owned by different user")
    public void cannotMoveToUnauthorizedParent() throws Exception {
        String user1Folder = fixtures.createFolderAsUser(TEST_USER_1_ID, "User 1's Folder");
        String user2Folder = fixtures.createFolderAsUser(TEST_USER_2_ID, "User 2's Folder");

        String moveRequest = """
            {
                "parentFolderId": "%s"
            }
            """.formatted(user2Folder);

        mockMvc.perform(patch("/api/docs/folders/" + user1Folder + "/move")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(moveRequest))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("EDGE: User folders isolated from other users")
    public void userFoldersAreIsolated() throws Exception {
        String user1Folder = fixtures.createFolderAsUser(TEST_USER_1_ID, "User 1's Folder");
        String user2Folder = fixtures.createFolderAsUser(TEST_USER_2_ID, "User 2's Folder");

        mockMvc.perform(get("/api/docs/folders/roots")
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.folderId == '%s')]".formatted(user1Folder)).exists())
                .andExpect(jsonPath("$[?(@.folderId == '%s')]".formatted(user2Folder)).doesNotExist());

        mockMvc.perform(get("/api/docs/folders/roots")
                        .header(USER_ID_HEADER, TEST_USER_2_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.folderId == '%s')]".formatted(user2Folder)).exists())
                .andExpect(jsonPath("$[?(@.folderId == '%s')]".formatted(user1Folder)).doesNotExist());
    }

    @Test
    @DisplayName("EDGE: Moving folder chain maintains consistency")
    public void movingFolderChainMaintainsConsistency() throws Exception {
        String parent1 = fixtures.createFolderAsUser(TEST_USER_1_ID, "Parent 1");
        String parent2 = fixtures.createFolderAsUser(TEST_USER_1_ID, "Parent 2");
        String parent3 = fixtures.createFolderAsUser(TEST_USER_1_ID, "Parent 3");

        String folder = fixtures.createFolderAsUser(TEST_USER_1_ID, "Traveling Folder", parent1);

        fixtures.verifyFolderParent(folder, parent1, TEST_USER_1_ID);

        String moveToParent2 = """
            {
                "parentFolderId": "%s"
            }
            """.formatted(parent2);

        mockMvc.perform(patch("/api/docs/folders/" + folder + "/move")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(moveToParent2))
                .andExpect(status().isOk());

        fixtures.verifyFolderParent(folder, parent2, TEST_USER_1_ID);

        String moveToParent3 = """
            {
                "parentFolderId": "%s"
            }
            """.formatted(parent3);

        mockMvc.perform(patch("/api/docs/folders/" + folder + "/move")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(moveToParent3))
                .andExpect(status().isOk());

        fixtures.verifyFolderParent(folder, parent3, TEST_USER_1_ID);
    }
}