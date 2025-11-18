<template>
  <div class="space-y-6">
    <h2 class="text-2xl font-bold text-content-primary dark:text-content-inverse mb-4">
      Page Management Tests
    </h2>
    
    <!-- Authentication Warning -->
    <AuthWarning 
      v-if="!getCurrentUserId()" 
      message="Page operations require authentication. Please login first using the Auth section above."
    />
    
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
      <!-- Get All Pages -->
      <TestCard
        title="Get All Pages"
        description="GET /api/docs/pages"
        button-text="Test Get All Pages"
        :is-loading="testManager.isTestLoading('pages-get-all')"
        :result="formatResult('pages-get-all')"
        @test="testGetAllPages"
        @clear-result="clearResult('pages-get-all')"
      />

      <!-- Create Page -->
      <TestCard
        title="Create Page"
        description="POST /api/docs/pages"
        button-text="Test Create Page"
        :form-fields="createPageFormFields"
        :is-loading="testManager.isTestLoading('pages-create')"
        :result="formatResult('pages-create')"
        @test="testCreatePage"
        @clear-result="clearResult('pages-create')"
        @field-update="updateFormField('createPage', $event)"
      />

      <!-- Get Page -->
      <TestCard
        title="Get Page by ID"
        description="GET /api/docs/pages/{pageId}"
        button-text="Test Get Page"
        :form-fields="getPageFormFields"
        :is-loading="testManager.isTestLoading('pages-get')"
        :result="formatResult('pages-get')"
        @test="testGetPage"
        @clear-result="clearResult('pages-get')"
        @field-update="updateFormField('updatePage', $event)"
      />

      <!-- Update Page -->
      <TestCard
        title="Update Page"
        description="PUT /api/docs/pages/{pageId}"
        button-text="Test Update Page"
        :form-fields="updatePageFormFields"
        :is-loading="testManager.isTestLoading('pages-update')"
        :result="formatResult('pages-update')"
        @test="testUpdatePage"
        @clear-result="clearResult('pages-update')"
        @field-update="updateFormField('updatePage', $event)"
      />

      <!-- Rename Page -->
      <TestCard
        title="Rename Page"
        description="PATCH /api/docs/pages/{pageId}"
        button-text="Test Rename Page"
        :form-fields="renamePageFormFields"
        :is-loading="testManager.isTestLoading('pages-rename')"
        :result="formatResult('pages-rename')"
        @test="testRenamePage"
        @clear-result="clearResult('pages-rename')"
        @field-update="updateFormField('renamePage', $event)"
      />

      <!-- Move Page -->
      <TestCard
        title="Move Page"
        description="PATCH /api/docs/pages/{pageId}/move"
        button-text="Test Move Page"
        :form-fields="movePageFormFields"
        :is-loading="testManager.isTestLoading('pages-move')"
        :result="formatResult('pages-move')"
        @test="testMovePage"
        @clear-result="clearResult('pages-move')"
        @field-update="updateFormField('movePage', $event)"
      />

      <!-- Delete Page -->
      <TestCard
        title="Delete Page"
        description="DELETE /api/docs/pages/{pageId}"
        button-text="Test Delete Page"
        loading-text="Deleting..."
        button-color="danger"
        warning="This will permanently delete the page!"
        :is-loading="testManager.isTestLoading('pages-delete')"
        :result="formatResult('pages-delete')"
        @test="testDeletePage"
        @clear-result="clearResult('pages-delete')"
      />
    </div>
  </div>
</template>

<script>
import TestCard from './TestCard.vue';
import AuthWarning from './AuthWarning.vue';
import { pageController } from '@/controllers/index.js';
import auth from '@/utils/auth';

export default {
  name: 'PageSection',
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
        createPage: { title: 'Test Page', folderId: null },
        updatePage: { pageId: '', content: 'Updated content' },
        renamePage: { pageId: '', newTitle: 'Renamed Page' },
        movePage: { pageId: '', folderId: '' }
      }
    }
  },
  computed: {
    createPageFormFields() {
      return [
        {
          key: 'title',
          label: 'Title',
          type: 'text',
          value: this.testForms.createPage.title
        },
        {
          key: 'folderId',
          label: 'Folder ID (optional)',
          type: 'text',
          value: this.testForms.createPage.folderId || ''
        }
      ];
    },
    getPageFormFields() {
      return [
        {
          key: 'pageId',
          label: 'Page ID',
          type: 'text',
          value: this.testForms.updatePage.pageId
        }
      ];
    },
    updatePageFormFields() {
      return [
        {
          key: 'content',
          label: 'Content',
          type: 'textarea',
          value: this.testForms.updatePage.content
        }
      ];
    },
    renamePageFormFields() {
      return [
        {
          key: 'pageId',
          label: 'Page ID',
          type: 'text',
          value: this.testForms.renamePage.pageId
        },
        {
          key: 'newTitle',
          label: 'New Title',
          type: 'text',
          value: this.testForms.renamePage.newTitle
        }
      ];
    },
    movePageFormFields() {
      return [
        {
          key: 'pageId',
          label: 'Page ID',
          type: 'text',
          value: this.testForms.movePage.pageId
        },
        {
          key: 'folderId',
          label: 'Target Folder ID',
          type: 'text',
          value: this.testForms.movePage.folderId
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

    async testGetAllPages() {
      await this.testManager.executeTest('pages-get-all', () => 
        pageController.getAllPages()
      );
    },

    async testCreatePage() {
      await this.testManager.executeTest('pages-create', () => 
        pageController.createPage(this.testForms.createPage)
      );
    },

    async testGetPage() {
      if (!this.testForms.updatePage.pageId) {
        alert('Please enter a Page ID first');
        return;
      }
      await this.testManager.executeTest('pages-get', () => 
        pageController.getPage(this.testForms.updatePage.pageId)
      );
    },

    async testUpdatePage() {
      if (!this.testForms.updatePage.pageId) {
        alert('Please enter a Page ID first');
        return;
      }
      await this.testManager.executeTest('pages-update', () => 
        pageController.updatePage(this.testForms.updatePage.pageId, {
          content: this.testForms.updatePage.content
        })
      );
    },

    async testRenamePage() {
      if (!this.testForms.renamePage.pageId) {
        alert('Please enter a Page ID first');
        return;
      }
      await this.testManager.executeTest('pages-rename', () => 
        pageController.renamePage(this.testForms.renamePage.pageId, {
          newTitle: this.testForms.renamePage.newTitle
        })
      );
    },

    async testMovePage() {
      if (!this.testForms.movePage.pageId) {
        alert('Please enter a Page ID first');
        return;
      }
      await this.testManager.executeTest('pages-move', () => 
        pageController.movePage(this.testForms.movePage.pageId, {
          folderId: this.testForms.movePage.folderId
        })
      );
    },

    async testDeletePage() {
      if (!this.testForms.updatePage.pageId) {
        alert('Please enter a Page ID first');
        return;
      }
      if (!confirm('Are you sure you want to delete this page?')) return;
      
      await this.testManager.executeTest('pages-delete', () => 
        pageController.deletePage(this.testForms.updatePage.pageId)
      );
    }
  }
}
</script>