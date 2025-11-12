
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
  <div class="min-h-screen bg-surface-light dark:bg-surface-dark transition-colors">
    <!-- Header -->
    <nav class="bg-white dark:bg-surface-dark-secondary shadow-sm border-b border-border-light dark:border-border-dark">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex justify-between h-16">
          <div class="flex items-center">
            <h1 class="text-2xl font-semibold text-content-primary dark:text-content-inverse">
              📁 Papairs Drive
            </h1>
          </div>
          <div class="flex items-center space-x-4">
            <button 
              @click="toggleTheme"
              class="p-2 rounded-md text-content-primary dark:text-content-inverse hover:bg-surface-light-secondary dark:hover:bg-surface-dark"
            >
              {{ isDark ? '☀️' : '🌙' }}
            </button>
            <button 
              @click="$router.push('/')"
              class="text-content-secondary hover:text-content-primary dark:hover:text-content-inverse px-3 py-2 rounded-md"
            >
              Back to Home
            </button>
          </div>
        </div>
      </div>
    </nav>

    <!-- Breadcrumbs -->
    <div class="bg-white dark:bg-surface-dark-secondary border-b border-border-light dark:border-border-dark">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-3">
        <nav class="flex items-center space-x-2 text-sm">
          <button 
            @click="navigateToRoot"
            class="text-accent hover:text-[#E66900] font-medium"
          >
            My Drive
          </button>
          <template v-for="(folder, index) in breadcrumbs" :key="folder.folderId">
            <span class="text-content-secondary">/</span>
            <button 
              @click="navigateToFolder(folder.folderId)"
              :class="index === breadcrumbs.length - 1 
                ? 'text-content-primary dark:text-content-inverse font-medium' 
                : 'text-accent hover:text-[#E66900]'"
            >
              {{ folder.name }}
            </button>
          </template>
        </nav>
      </div>
    </div>

    <!-- Toolbar -->
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
      <div class="flex items-center justify-between mb-6">
        <h2 class="text-xl font-semibold text-content-primary dark:text-content-inverse">
          {{ currentFolder ? currentFolder.name : 'My Drive' }}
        </h2>
        <div class="flex items-center space-x-3">
          <button 
            @click="showCreateFolderModal = true"
            class="flex items-center space-x-2 px-4 py-2 bg-white dark:bg-surface-dark-secondary border border-border-light dark:border-border-dark rounded-lg hover:bg-surface-light-secondary dark:hover:bg-surface-dark text-content-primary dark:text-content-inverse transition-colors"
          >
            <span class="text-lg">📁</span>
            <span>New Folder</span>
          </button>
          <button 
            @click="showCreateDocModal = true"
            class="flex items-center space-x-2 px-4 py-2 bg-accent hover:bg-[#E66900] text-white rounded-lg transition-colors"
          >
            <span class="text-lg">📄</span>
            <span>New Document</span>
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
        <p class="text-content-secondary">
          Create a new folder or document to get started
        </p>
      </div>

      <!-- Content Grid -->
      <div v-else class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-4">
        <!-- Folders -->
        <FolderCard 
          v-for="folder in folders" 
          :key="folder.folderId"
          :folder="folder"
          @click="navigateToFolder(folder.folderId)"
          @delete="deleteFolder(folder.folderId)"
          @rename="startRenameFolder(folder)"
        />

        <!-- Documents -->
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