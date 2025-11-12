<script>
import { useTheme } from '@/composables/useTheme';
import { ApiTestManager } from '@/controllers/ApiTestManager.js';
import AuthSection from '@/components/controllerComponents/AuthSection.vue';
import DocsSection from '@/components/controllerComponents/DocsSection.vue';
import PageSection from '@/components/controllerComponents/PageSection.vue';
import FolderSection from '@/components/controllerComponents/FolderSection.vue';

export default {
  name: 'HomeView',
  components: {
    AuthSection,
    DocsSection,
    PageSection,
    FolderSection
  },
  data() {
    return {
      // Test manager
      testManager: new ApiTestManager(),
      
      // Active test section
      activeSection: 'auth'
    }
  },
  setup() {
    const { isDark, toggleTheme, initTheme } = useTheme();
    return { isDark, toggleTheme, initTheme }
  },
  mounted() {
    this.initTheme();
  },
  computed: {
  },
  methods: {
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
              to="/docs" 
              class="text-content-secondary hover:text-content-primary dark:hover:text-content-inverse px-3 py-2 rounded-md"
            >
              Docs
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
            v-for="section in ['auth', 'docs', 'pages', 'folders']" 
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
