<script>
import axios from 'axios'
 import auth from '@/utils/auth'
import { useTheme } from '@/composables/useTheme';
import { ApiTestManager } from '@/controllers/ApiTestManager.js';
import AuthSection from '@/components/controllerComponents/AuthSection.vue';
import DocsSection from '@/components/controllerComponents/DocsSection.vue';
import PageSection from '@/components/controllerComponents/PageSection.vue';
import FolderSection from '@/components/controllerComponents/FolderSection.vue';
import AISection from '@/components/controllerComponents/AISection.vue';
  import { driveService } from '@/utils/driveService'

  export default {
    name: 'HomeView',
    components: {
    AuthSection,
    DocsSection,
    PageSection,
    FolderSection,
    AISection
  },
    data() {
      return {
        authResult: null,
        docsResult: null,
        driveResult: null,
        pageName: '',
        pageResult: null,
        pageError: null,
        creatingPage: false,
        testManager: new ApiTestManager(),
      
        // Active test section
        activeSection: 'auth'
      }
    },
    setup() {
      const { isDark, toggleTheme, initTheme } = useTheme();
      
      return {
        isDark,
        toggleTheme,
        initTheme,
      }
    },
    mounted() {
      this.initTheme();
    },
    methods: {
      async testAuth() {
        try {
          const response = await axios.get('http://localhost:8081/api/auth/health');
          this.authResult = JSON.stringify(response.data, null, 2);
        } catch (error) {
          this.authResult = `Error: ${error.message}`;
        }
      },
      async testDocs() {
        try {
          const response = await axios.get('http://localhost:8082/api/docs/health');
          this.docsResult = JSON.stringify(response.data, null, 2);
        } catch (error) {
          this.docsResult = `Error: ${error.message}`;
        }
      },
      async testDrive() {
        try {
          // Test fetching folders and documents
          const folders = await driveService.getRootFolders();
          const documents = await driveService.getAllDocuments();
          
          this.driveResult = JSON.stringify({
            foldersCount: folders.length,
            documentsCount: documents.length,
            folders: folders.slice(0, 3), // Show first 3 folders
            documents: documents.slice(0, 3) // Show first 3 documents
          }, null, 2);
        } catch (error) {
          console.error('Drive test error:', error);
          this.driveResult = `Error: ${error.response?.data?.message || error.message}`;
        }
      },
      async createPage() {
        // Reset previous results
        this.pageResult = null
        this.pageError = null
        
        // Validate inputs
        if (!this.pageName.trim()) {
          this.pageError = 'Page name is required'
          return
        }
        
        const userId = auth.getUserId()
        console.log('User ID from auth.getUserId():', userId)
        console.log('User data from localStorage:', auth.getUser())
        console.log('Auth headers:', auth.getUserIdHeader())
        
        // Temporary workaround: use a known user ID if no user is logged in
        const actualUserId = userId
        console.log('Using user ID:', actualUserId)
        if (!actualUserId) {
          this.pageError = 'You must be logged in to create a page'
          return
        }
        
        this.creatingPage = true
        
        try {
          const response = await axios.post('http://localhost:8082/api/docs/pages', {
            title: this.pageName.trim(),
            folderId: null // Optional - no folder for now
          }, {
            headers: {
              'X-User-Id': actualUserId, // Use the fallback user ID
              'Content-Type': 'application/json'
            }
          })
          
          this.pageResult = response.data
          this.pageName = '' // Clear the form
          
          console.log('Page created successfully:', response.data)
          
        } catch (error) {
          console.error('Error creating page:', error)
          this.pageError = error.response?.data?.message || error.message || 'Failed to create page'
        } finally {
          this.creatingPage = false
        }
      }
    }
  }
</script>

<template>
  <div class="bg-surface-light dark:bg-surface-dark transition-colors min-h-screen">
    <!-- Navigation -->
    <nav class="bg-surface-light dark:bg-surface-dark-secondary shadow-lg">
      <div class="max-w-7xl mx-auto px-4">
        <div class="flex justify-between h-16">
          <div class="flex items-center">
            <h1 class="text-xl font-bold text-content-primary dark:text-content-inverse">
              Papairs API Tester
            </h1>
          </div>
          <div class="flex items-center space-x-4">
            <router-link 
              to="/" 
              class="text-content-secondary hover:text-content-primary dark:hover:text-content-inverse px-3 py-2 rounded-md"
            >
              Home
            </router-link>
            <router-link 
              to="/ai" 
              class="text-content-secondary hover:text-content-primary dark:hover:text-content-inverse px-3 py-2 rounded-md"
            >
              AI
            </router-link>
            <router-link 
              to="/docs" 
              class="text-content-secondary hover:text-content-primary dark:hover:text-content-inverse px-3 py-2 rounded-md"
            >
              Docs
            </router-link>
            <router-link 
              to="/drive" 
              class="text-content-secondary hover:text-content-primary dark:hover:text-content-inverse px-3 py-2 rounded-md"
            >
              Drive
            </router-link>
            <button 
              @click="toggleTheme"
              class="p-2 rounded-md text-content-primary dark:text-content-inverse hover:bg-surface-light-secondary dark:hover:bg-surface-dark"
              :aria-label="isDark ? 'Switch to light mode' : 'Switch to dark mode'"
            >
              {{ isDark ? '☀️' : '🌙' }}
            </button>
            
            <router-link 
              to="/login" 
              class="bg-accent hover:bg-[#E66900] text-content-inverse font-bold py-2 px-4 rounded"
            >
              Login
            </router-link>
          </div>
        </div>
      </div>
    </nav>

    <!-- Main Content -->
    <div class="max-w-7xl mx-auto px-4 py-8">
      <!-- Header -->
      <div class="text-center mb-8">
        <h1 class="text-4xl font-bold text-content-primary dark:text-content-inverse mb-4">
          API Endpoint Testing Dashboard
        </h1>
        <p class="text-lg text-content-secondary mb-6">
          Comprehensive testing interface for all backend endpoints
        </p>
      </div>

      <!-- Section Tabs -->
      <div class="mb-8 border-b border-border-light dark:border-border-dark">
        <nav class="flex space-x-8">
          <button 
            v-for="section in ['auth', 'ai', 'docs', 'pages', 'folders']" 
            :key="section"
            @click="activeSection = section"
            :class="[
              'py-2 px-1 border-b-2 font-medium text-sm transition-colors',
              activeSection === section 
                ? 'border-accent text-accent' 
                : 'border-transparent text-content-secondary hover:text-content-primary hover:border-gray-300'
            ]"
          >
            {{ section.charAt(0).toUpperCase() + section.slice(1) }}
          </button>
        </nav>
      </div>

      <!-- Test Sections -->
      <AuthSection 
        v-if="activeSection === 'auth'" 
        :test-manager="testManager"
      />
      
      <AISection 
        v-if="activeSection === 'ai'" 
        :test-manager="testManager"
      />
      
      <DocsSection 
        v-if="activeSection === 'docs'" 
        :test-manager="testManager"
      />
      
      <PageSection 
        v-if="activeSection === 'pages'" 
        :test-manager="testManager"
      />
      
      <FolderSection 
        v-if="activeSection === 'folders'" 
        :test-manager="testManager"
      />
    </div>
  </div>
</template>

<style scoped>
/* Only layout-specific styles remain here */
</style>
