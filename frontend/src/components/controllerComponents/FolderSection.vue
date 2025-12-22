<script>
import TestCard from './TestCard.vue';
import AuthWarning from './AuthWarning.vue';
import { folderController } from '@/controllers/index.js';
import auth from '@/utils/auth';

export default {
  name: 'FolderSection',
  components: {
    TestCard,
    AuthWarning
  },
  props: {
    testManager: {
      type: Object,
      required: true
    }
  },
  data() {
    return {
      testForms: {
        createFolder: { name: 'Test Folder', parentFolderId: null },
        renameFolder: { folderId: '', newName: 'Renamed Folder' },
        moveFolder: { folderId: '', parentFolderId: '' }
      }
    }
  },
  computed: {
    createFolderFormFields() {
      return [
        {
          key: 'name',
          label: 'Name',
          type: 'text',
          value: this.testForms.createFolder.name
        },
        {
          key: 'parentFolderId',
          label: 'Parent Folder ID (optional)',
          type: 'text',
          value: this.testForms.createFolder.parentFolderId || ''
        }
      ];
    },
    getFolderFormFields() {
      return [
        {
          key: 'folderId',
          label: 'Folder ID',
          type: 'text',
          value: this.testForms.renameFolder.folderId
        }
      ];
    },
    renameFolderFormFields() {
      return [
        {
          key: 'newName',
          label: 'New Name',
          type: 'text',
          value: this.testForms.renameFolder.newName
        }
      ];
    },
    moveFolderFormFields() {
      return [
        {
          key: 'folderId',
          label: 'Folder ID',
          type: 'text',
          value: this.testForms.moveFolder.folderId
        },
        {
          key: 'parentFolderId',
          label: 'Parent Folder ID',
          type: 'text',
          value: this.testForms.moveFolder.parentFolderId
        }
      ];
    }
  },
  methods: {
    getCurrentUserId() {
      return auth.getUserId();
    },

    formatResult(testKey) {
      const result = this.testManager.getResult(testKey);
      return result ? this.testManager.formatResult(result) : null;
    },
    
    clearResult(testKey) {
      this.testManager.clearResult(testKey);
    },

    updateFormField(formName, { key, value }) {
      if (this.testForms[formName]) {
        this.testForms[formName][key] = value;
      }
    },

    async testGetAllFolders() {
      await this.testManager.executeTest('folders-get-all', () => 
        folderController.getAllFolders()
      );
    },

    async testGetRootFolders() {
      await this.testManager.executeTest('folders-get-roots', () => 
        folderController.getRootFolders()
      );
    },

    async testCreateFolder() {
      await this.testManager.executeTest('folders-create', () => 
        folderController.createFolder(this.testForms.createFolder)
      );
    },

    async testGetFolder() {
      if (!this.testForms.renameFolder.folderId) {
        alert('Please enter a Folder ID first');
        return;
      }
      await this.testManager.executeTest('folders-get', () => 
        folderController.getFolder(this.testForms.renameFolder.folderId)
      );
    },

    async testRenameFolder() {
      if (!this.testForms.renameFolder.folderId) {
        alert('Please enter a Folder ID first');
        return;
      }
      await this.testManager.executeTest('folders-rename', () => 
        folderController.renameFolder(this.testForms.renameFolder.folderId, {
          newName: this.testForms.renameFolder.newName
        })
      );
    },

    async testMoveFolder() {
      if (!this.testForms.moveFolder.folderId) {
        alert('Please enter a Folder ID first');
        return;
      }
      await this.testManager.executeTest('folders-move', () => 
        folderController.moveFolder(this.testForms.moveFolder.folderId, {
          parentFolderId: this.testForms.moveFolder.parentFolderId
        })
      );
    },

    async testDeleteFolder() {
      if (!this.testForms.renameFolder.folderId) {
        alert('Please enter a Folder ID first');
        return;
      }
      if (!confirm('Are you sure you want to delete this folder?')) return;
      
      await this.testManager.executeTest('folders-delete', () => 
        folderController.deleteFolder(this.testForms.renameFolder.folderId, false)
      );
    },

    async testGetFolderTrees() {
      await this.testManager.executeTest('folders-get-trees', () => 
        folderController.getUserFolderTrees()
      );
    },

    async testGetFolderPath() {
      if (!this.testForms.renameFolder.folderId) {
        alert('Please enter a Folder ID first');
        return;
      }
      await this.testManager.executeTest('folders-get-path', () => 
        folderController.getFolderPath(this.testForms.renameFolder.folderId)
      );
    }
  }
}
</script>

<template>
  <div class="space-y-6">
    <h2 class="text-2xl font-bold text-content-primary mb-4">
      Folder Management Tests
    </h2>
    
    <!-- Authentication Warning -->
    <AuthWarning 
      v-if="!getCurrentUserId()" 
      message="Folder operations require authentication. Please login first using the Auth section above."
    />
    
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
      <!-- Get All Folders -->
      <TestCard
        title="Get All Folders"
        description="GET /api/docs/folders"
        button-text="Test Get All Folders"
        :is-loading="testManager.isTestLoading('folders-get-all')"
        :result="formatResult('folders-get-all')"
        @test="testGetAllFolders"
        @clear-result="clearResult('folders-get-all')"
      />

      <!-- Get Root Folders -->
      <TestCard
        title="Get Root Folders"
        description="GET /api/docs/folders/roots"
        button-text="Test Get Root Folders"
        :is-loading="testManager.isTestLoading('folders-get-roots')"
        :result="formatResult('folders-get-roots')"
        @test="testGetRootFolders"
        @clear-result="clearResult('folders-get-roots')"
      />

      <!-- Create Folder -->
      <TestCard
        title="Create Folder"
        description="POST /api/docs/folders"
        button-text="Test Create Folder"
        :form-fields="createFolderFormFields"
        :is-loading="testManager.isTestLoading('folders-create')"
        :result="formatResult('folders-create')"
        @test="testCreateFolder"
        @clear-result="clearResult('folders-create')"
        @field-update="updateFormField('createFolder', $event)"
      />

      <!-- Get Folder -->
      <TestCard
        title="Get Folder by ID"
        description="GET /api/docs/folders/{folderId}"
        button-text="Test Get Folder"
        :form-fields="getFolderFormFields"
        :is-loading="testManager.isTestLoading('folders-get')"
        :result="formatResult('folders-get')"
        @test="testGetFolder"
        @clear-result="clearResult('folders-get')"
        @field-update="updateFormField('renameFolder', $event)"
      />

      <!-- Rename Folder -->
      <TestCard
        title="Rename Folder"
        description="PATCH /api/docs/folders/{folderId}"
        button-text="Test Rename Folder"
        :form-fields="renameFolderFormFields"
        :is-loading="testManager.isTestLoading('folders-rename')"
        :result="formatResult('folders-rename')"
        @test="testRenameFolder"
        @clear-result="clearResult('folders-rename')"
        @field-update="updateFormField('renameFolder', $event)"
      />

      <!-- Move Folder -->
      <TestCard
        title="Move Folder"
        description="PATCH /api/docs/folders/{folderId}/move"
        button-text="Test Move Folder"
        :form-fields="moveFolderFormFields"
        :is-loading="testManager.isTestLoading('folders-move')"
        :result="formatResult('folders-move')"
        @test="testMoveFolder"
        @clear-result="clearResult('folders-move')"
        @field-update="updateFormField('moveFolder', $event)"
      />

      <!-- Get Folder Trees -->
      <TestCard
        title="Get Folder Trees"
        description="GET /api/docs/folders/trees"
        button-text="Test Get Folder Trees"
        :is-loading="testManager.isTestLoading('folders-get-trees')"
        :result="formatResult('folders-get-trees')"
        @test="testGetFolderTrees"
        @clear-result="clearResult('folders-get-trees')"
      />

      <!-- Get Folder Path -->
      <TestCard
        title="Get Folder Path"
        description="GET /api/docs/folders/{folderId}/path"
        button-text="Test Get Folder Path"
        :is-loading="testManager.isTestLoading('folders-get-path')"
        :result="formatResult('folders-get-path')"
        @test="testGetFolderPath"
        @clear-result="clearResult('folders-get-path')"
      />

      <!-- Delete Folder -->
      <TestCard
        title="Delete Folder"
        description="DELETE /api/docs/folders/{folderId}"
        button-text="Test Delete Folder"
        loading-text="Deleting..."
        button-color="danger"
        warning="This will permanently delete the folder!"
        :is-loading="testManager.isTestLoading('folders-delete')"
        :result="formatResult('folders-delete')"
        @test="testDeleteFolder"
        @clear-result="clearResult('folders-delete')"
      />
    </div>
  </div>
</template>

