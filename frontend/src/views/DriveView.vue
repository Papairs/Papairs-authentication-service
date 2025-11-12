
<script>
import { ref, onMounted} from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useTheme } from '@/composables/useTheme'
import FolderCard from '@/components/FolderCard.vue'
import DocumentCard from '@/components/DocumentCard.vue'
import CreateFolderModal from '@/components/CreateFolderModal.vue'
import CreateDocumentModal from '@/components/CreateDocumentModal.vue'
import RenameFolderModal from '@/components/RenameFolderModal.vue'
import RenameDocumentModal from '@/components/RenameDocumentModal.vue'
import { driveService } from '@/utils/driveService'

export default {
  name: 'DriveView',
  components: {
    FolderCard,
    DocumentCard,
    CreateFolderModal,
    CreateDocumentModal,
    RenameFolderModal,
    RenameDocumentModal
  },
  setup() {
    const router = useRouter()
    const route = useRoute()
    const { isDark, toggleTheme, initTheme } = useTheme()

    const loading = ref(false)
    const folders = ref([])
    const documents = ref([])
    const currentFolderId = ref(null)
    const currentFolder = ref(null)
    const breadcrumbs = ref([])

    const showCreateFolderModal = ref(false)
    const showCreateDocModal = ref(false)
    const showRenameFolderModal = ref(false)
    const showRenameDocModal = ref(false)
    const folderToRename = ref(null)
    const documentToRename = ref(null)
    const showNotebookDropdown = ref(false)

    const loadContent = async (folderId = null) => {
      loading.value = true
      try {
        currentFolderId.value = folderId

        // Load folders
        if (folderId) {
          folders.value = await driveService.getChildFolders(folderId)
          currentFolder.value = await driveService.getFolder(folderId)
          breadcrumbs.value = await driveService.getFolderPath(folderId)
        } else {
          folders.value = await driveService.getRootFolders()
          currentFolder.value = null
          breadcrumbs.value = []
        }

        // Load documents - filter by current folder
        const allDocuments = await driveService.getAllDocuments()
        documents.value = allDocuments.filter(doc => doc.folderId === folderId)
      } catch (error) {
        console.error('Error loading content:', error)
        alert('Failed to load content. Please try again.')
      } finally {
        loading.value = false
      }
    }

    const navigateToRoot = () => {
      router.push('/drive')
      loadContent(null)
    }

    const navigateToFolder = (folderId) => {
      router.push(`/drive/${folderId}`)
      loadContent(folderId)
    }

    const openDocument = (pageId) => {
      router.push(`/docs/${pageId}`)
    }

    const deleteFolder = async (folderId) => {
      if (!confirm('Are you sure you want to delete this folder? This will delete all contents inside.')) {
        return
      }
      try {
        await driveService.deleteFolder(folderId, true)
        await loadContent(currentFolderId.value)
      } catch (error) {
        console.error('Error deleting folder:', error)
        alert('Failed to delete folder. Please try again.')
      }
    }

    const deleteDocument = async (pageId) => {
      if (!confirm('Are you sure you want to delete this document?')) {
        return
      }
      try {
        await driveService.deleteDocument(pageId)
        await loadContent(currentFolderId.value)
      } catch (error) {
        console.error('Error deleting document:', error)
        alert('Failed to delete document. Please try again.')
      }
    }

    const startRenameFolder = (folder) => {
      folderToRename.value = folder
      showRenameFolderModal.value = true
    }

    const startRenameDocument = (doc) => {
      documentToRename.value = doc
      showRenameDocModal.value = true
    }

    const onFolderCreated = () => {
      showCreateFolderModal.value = false
      loadContent(currentFolderId.value)
    }

    const onDocumentCreated = (document) => {
      showCreateDocModal.value = false
      // Navigate to the new document
      router.push(`/docs/${document.pageId}`)
    }

    const onFolderRenamed = () => {
      showRenameFolderModal.value = false
      folderToRename.value = null
      loadContent(currentFolderId.value)
    }

    const onDocumentRenamed = () => {
      showRenameDocModal.value = false
      documentToRename.value = null
      loadContent(currentFolderId.value)
    }

    onMounted(() => {
      initTheme()
      const folderId = route.params.folderId || null
      loadContent(folderId)
    })

    return {
      isDark,
      toggleTheme,
      loading,
      folders,
      documents,
      currentFolderId,
      currentFolder,
      breadcrumbs,
      showCreateFolderModal,
      showCreateDocModal,
      showRenameFolderModal,
      showRenameDocModal,
      showNotebookDropdown,
      folderToRename,
      documentToRename,
      navigateToRoot,
      navigateToFolder,
      openDocument,
      deleteFolder,
      deleteDocument,
      startRenameFolder,
      startRenameDocument,
      onFolderCreated,
      onDocumentCreated,
      onFolderRenamed,
      onDocumentRenamed
    }
  }
}
</script>

<template>
  <div class="flex h-screen bg-surface-light dark:bg-surface-dark transition-colors overflow-hidden">
    <!-- Sidebar -->
    <aside class="w-64 bg-white dark:bg-surface-dark-secondary border-r border-border-light dark:border-border-dark flex flex-col">
      <!-- User Profile -->
      <div class="p-4 border-border-light dark:border-border-dark">
        <div class="flex items-center space-x-3">
          <div class="w-8 h-8 rounded-full bg-accent flex items-center justify-center text-white font-semibold">
            G
          </div>
          <div class="flex-1">
            <p class="text-sm font-medium text-content-primary dark:text-content-inverse">Gustaw Juul</p>
            <p class="text-xs text-content-secondary">Student User</p>
          </div>
          <button 
            @click="toggleTheme"
            class="p-1 rounded hover:bg-surface-light-secondary dark:hover:bg-surface-dark"
          >
            {{ isDark ? '☀️' : '🌙' }}
          </button>
        </div>
      </div>

      <!-- New Button -->
      <div class="p-4">
        <button 
          @click="showCreateDocModal = true"
          class="w-full flex items-center justify-center space-x-2 px-4 py-2 bg-accent hover:bg-[#E66900] text-white rounded-lg transition-colors font-medium"
        >
          <span class="text-xl">+</span>
          <span>New</span>
        </button>
      </div>

      <!-- Navigation -->
      <nav class="flex-1 overflow-y-auto px-2">
        <div class="space-y-1">
          <button 
            @click="navigateToRoot"
            class="w-full flex items-center space-x-3 px-3 py-2 rounded-md hover:bg-surface-light-secondary dark:hover:bg-surface-dark text-content-primary dark:text-content-inverse transition-colors"
          >
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
            <span class="text-sm">Search</span>
          </button>
          
          <button 
            @click="navigateToRoot"
            class="w-full flex items-center space-x-3 px-3 py-2 rounded-md hover:bg-surface-light-secondary dark:hover:bg-surface-dark text-content-primary dark:text-content-inverse transition-colors"
          >
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
            </svg>
            <span class="text-sm">Home</span>
          </button>

          <button 
            class="w-full flex items-center space-x-3 px-3 py-2 rounded-md hover:bg-surface-light-secondary dark:hover:bg-surface-dark text-content-primary dark:text-content-inverse transition-colors"
          >
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
            </svg>
            <span class="text-sm">Invite Friends</span>
          </button>
        </div>

        <!-- My Papairs Section -->
        <div class="mt-6">
          <p class="px-3 text-xs font-semibold text-content-secondary uppercase tracking-wider mb-2">
            My papairs
          </p>
          <button 
            class="w-full flex items-center space-x-3 px-3 py-2 rounded-md bg-accent/10 text-accent transition-colors"
          >
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
            </svg>
            <span class="text-sm font-medium">My notebook</span>
          </button>

          <button 
            class="w-full flex items-center space-x-3 px-3 py-2 rounded-md hover:bg-surface-light-secondary dark:hover:bg-surface-dark text-content-primary dark:text-content-inverse transition-colors mt-1"
          >
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7H5a2 2 0 00-2 2v9a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-3m-1 4l-3 3m0 0l-3-3m3 3V4" />
            </svg>
            <span class="text-sm">Shared with me</span>
          </button>

          <button 
            class="w-full flex items-center space-x-3 px-3 py-2 rounded-md hover:bg-surface-light-secondary dark:hover:bg-surface-dark text-content-primary dark:text-content-inverse transition-colors mt-1"
          >
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11.049 2.927c.3-.921 1.603-.921 1.902 0l1.519 4.674a1 1 0 00.95.69h4.915c.969 0 1.371 1.24.588 1.81l-3.976 2.888a1 1 0 00-.363 1.118l1.518 4.674c.3.922-.755 1.688-1.538 1.118l-3.976-2.888a1 1 0 00-1.176 0l-3.976 2.888c-.783.57-1.838-.197-1.538-1.118l1.518-4.674a1 1 0 00-.363-1.118l-3.976-2.888c-.784-.57-.38-1.81.588-1.81h4.914a1 1 0 00.951-.69l1.519-4.674z" />
            </svg>
            <span class="text-sm">Favorites</span>
          </button>
        </div>
      </nav>

      <!-- Settings at Bottom -->
      <div class="p-4">
        <button 
          class="w-full flex items-center space-x-3 px-3 py-2 rounded-md hover:bg-surface-light-secondary dark:hover:bg-surface-dark text-content-primary dark:text-content-inverse transition-colors"
        >
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
            </svg>
          <span class="text-sm">Settings</span>
        </button>
        <button 
          class="w-full flex items-center space-x-3 px-3 py-2 rounded-md hover:bg-surface-light-secondary dark:hover:bg-surface-dark text-content-primary dark:text-content-inverse transition-colors mt-1"
        >
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
          </svg>
          <span class="text-sm">Trash</span>
        </button>
      </div>
    </aside>

    <!-- Main Content -->
    <main class="flex-1 overflow-y-auto">
      <!-- Top Bar -->
      <div class="bg-white dark:bg-surface-dark-secondary border-border-light dark:border-border-dark">
        <div class="px-8 py-4">
          <div class="flex items-center justify-between">
            <!-- Search -->
            <div class="flex-1 max-w-2xl">
              <div class="relative" style="max-width: 500px; height: 48px; gap: 24px;">
                <input 
                  type="text" 
                  placeholder="Search in Papairs"
                  class="w-full h-full bg-[#F3F3F3] dark:bg-surface-dark text-content-primary dark:text-content-inverse focus:outline-none focus:ring-2 focus:ring-accent"
                  style="border: 2px solid #202020BF; border-radius: 100px; padding-left: 56px; padding-right: 24px;"
                />
                <svg class="absolute left-6 top-1/2 transform -translate-y-1/2 w-5 h-5 text-content-secondary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                </svg>
              </div>
            </div>

            <!-- User Actions -->
            <div class="flex items-center space-x-4">
              <button 
                @click="$router.push('/')"
                class="text-sm text-content-secondary hover:text-content-primary dark:hover:text-content-inverse"
              >
                Home
              </button>
            </div>
          </div>
        </div>

        <!-- Section Header with Dropdown -->
        <div class="px-8 py-3 border-t-2 border-accent">
          <div class="flex items-center justify-between">
            <div class="relative">
              <button 
                @click="showNotebookDropdown = !showNotebookDropdown"
                class="flex items-center space-x-2 text-2xl font-semibold text-content-primary dark:text-content-inverse hover:bg-surface-light-secondary dark:hover:bg-surface-dark px-2 py-1 rounded"
              >
                <span>{{ currentFolder ? currentFolder.name : 'My Notebooks' }}</span>
                <svg class="w-5 h-5 transition-transform" :class="{ 'rotate-180': showNotebookDropdown }" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
                </svg>
              </button>

              <!-- Dropdown Menu -->
              <div 
                v-if="showNotebookDropdown"
                class="absolute left-0 mt-2 w-56 bg-white dark:bg-surface-dark-secondary border border-border-light dark:border-border-dark rounded-lg shadow-lg overflow-hidden z-10"
              >
                <button 
                  @click="showCreateDocModal = true; showNotebookDropdown = false"
                  class="w-full flex items-center space-x-3 px-4 py-3 hover:bg-surface-light dark:hover:bg-surface-dark text-content-primary dark:text-content-inverse transition-colors text-left"
                >
                  <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                  </svg>
                  <span class="text-sm font-medium">New Papair</span>
                </button>
                <button 
                  @click="showCreateFolderModal = true; showNotebookDropdown = false"
                  class="w-full flex items-center space-x-3 px-4 py-3 hover:bg-surface-light dark:hover:bg-surface-dark text-content-primary dark:text-content-inverse transition-colors text-left"
                >
                  <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z" />
                  </svg>
                  <span class="text-sm font-medium">New Folder</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Content Area -->
      <div class="pt-2 px-8 pb-8">
        <!-- Section Labels -->
        <div class="mb-6">
          <div class="flex items-center justify-between mb-4">
            <h3 class="text-sm font-medium text-accent">Notebooks</h3>
            <button 
              @click="showCreateFolderModal = true"
              class="text-sm text-content-secondary hover:text-accent"
            >
              + New Folder
            </button>
          </div>
        </div>

        <!-- Loading State -->
        <div v-if="loading" class="flex justify-center items-center py-20">
          <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-accent"></div>
        </div>

        <!-- Empty State -->
        <div v-else-if="folders.length === 0 && documents.length === 0" class="text-center py-20">
          <div class="text-6xl mb-4">📭</div>
          <h3 class="text-xl font-medium text-content-primary dark:text-content-inverse mb-2">
            No items yet
          </h3>
          <p class="text-content-secondary mb-4">
            Create a new folder or document to get started
          </p>
          <button 
            @click="showCreateDocModal = true"
            class="px-6 py-2 bg-accent hover:bg-[#E66900] text-white rounded-lg transition-colors"
          >
            Create First Document
          </button>
        </div>

        <!-- Content Grid -->
        <div v-else>
          <!-- Folders Grid -->
          <div v-if="folders.length > 0" class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-4 mb-8">
            <FolderCard 
              v-for="folder in folders" 
              :key="folder.folderId"
              :folder="folder"
              @click="navigateToFolder(folder.folderId)"
              @delete="deleteFolder(folder.folderId)"
              @rename="startRenameFolder(folder)"
            />
          </div>

          <!-- Documents Section -->
          <div v-if="documents.length > 0">
            <div class="flex items-center justify-between mb-4">
              <h3 class="text-sm font-medium text-accent">Papairs</h3>
            </div>
            <div class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-4">
              <DocumentCard 
                v-for="doc in documents" 
                :key="doc.pageId"
                :document="doc"
                @click="openDocument(doc.pageId)"
                @delete="deleteDocument(doc.pageId)"
                @rename="startRenameDocument(doc)"
              />
            </div>
          </div>
        </div>
      </div>
    </main>

    <!-- Create Folder Modal -->
    <CreateFolderModal 
      v-if="showCreateFolderModal"
      :parent-folder-id="currentFolderId"
      @close="showCreateFolderModal = false"
      @created="onFolderCreated"
    />

    <!-- Create Document Modal -->
    <CreateDocumentModal 
      v-if="showCreateDocModal"
      :folder-id="currentFolderId"
      @close="showCreateDocModal = false"
      @created="onDocumentCreated"
    />

    <!-- Rename Folder Modal -->
    <RenameFolderModal 
      v-if="showRenameFolderModal"
      :folder="folderToRename"
      @close="showRenameFolderModal = false"
      @renamed="onFolderRenamed"
    />

    <!-- Rename Document Modal -->
    <RenameDocumentModal 
      v-if="showRenameDocModal"
      :document="documentToRename"
      @close="showRenameDocModal = false"
      @renamed="onDocumentRenamed"
    />
  </div>
</template>