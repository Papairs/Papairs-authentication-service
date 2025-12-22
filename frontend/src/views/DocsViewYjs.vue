<script>
import TiptapEditorYjs from '@/components/TiptapEditorYjs.vue'
import FlashcardsPanel from '@/components/FlashcardsPanel.vue'
import ContextFilesPanel from '@/components/ContextFilesPanel.vue'
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useYjsDocument } from '@/composables/useYjsDocument'
import { API_BASE_URL } from '@/config'
import auth from '@/utils/auth'
import axios from 'axios'

export default {
  name: 'DocsView',
  components: { 
    TiptapEditorYjs,
    FlashcardsPanel,
    ContextFilesPanel
  },
  props: {
    id: {
      type: String,
      required: true
    }
  },
  setup(props) {
    // Use document ID from props
    const documentId = computed(() => props.id)
    
    // Initialize Y.js document
    const { ydoc, provider, isConnected, isSynced, error: yjsError, connect, disconnect } = useYjsDocument(documentId.value)
    
    const editor = ref(null)
    const isGenerating = ref(false)
    const hasError = ref(false)
    const errorMessage = ref('')
    const initialContent = ref(null)
    const isLoadingContent = ref(true)
    
    // Flashcard state
    const showSuccess = ref(false)
    const numberOfCards = ref(5)
    const showFlashcards = ref(false)
    const pageFlashcards = ref([])
    const isLoadingFlashcards = ref(false)

    // Context files state
    const showContextFiles = ref(false)
    const userFiles = ref([])
    const selectedFiles = ref([])
    const uploadError = ref(null)
    const uploadSuccess = ref(null)
    const isUploading = ref(false)
    const currentUploadFile = ref(null)
    const uploadAbortController = ref(null)
    const aiMode = ref('fast') // 'fast' or 'advanced'

    // Get user info for collaborative cursor
    const userId = auth.getUserId()
    const user = {
      name: userId || 'Anonymous',
      color: getUserColor(userId)
    }

    // Generate consistent color for user
    function getUserColor(userId) {
      const colors = [
        '#3b82f6', // blue
        '#10b981', // green
        '#f59e0b', // amber
        '#ef4444', // red
        '#8b5cf6', // violet
        '#ec4899', // pink
        '#14b8a6', // teal
        '#f97316', // orange
      ]
      
      let hash = 0
      for (let i = 0; i < userId.length; i++) {
        hash = userId.charCodeAt(i) + ((hash << 5) - hash)
      }
      
      return colors[Math.abs(hash) % colors.length]
    }

    // Watch for Y.js errors
    watch(yjsError, (error) => {
      if (error) {
        hasError.value = true
        errorMessage.value = error
      }
    })

    // Validate document ID
    if (!props.id) {
      hasError.value = true
      errorMessage.value = 'No document ID provided'
    }

    const handleEditorReady = (editorInstance) => {
      editor.value = editorInstance
    }

    async function generateFlashcards() {
      if (!editor.value) {
        alert('Editor not ready')
        return
      }

      const content = editor.value.getText().trim()
      if (!content || content.length < 50) {
        alert('Please write at least 50 characters before generating flashcards.')
        return
      }
      
      isGenerating.value = true
      showSuccess.value = false
      
      try {
        const aiResponse = await axios.post(`${API_BASE_URL}/api/ai/generate-flashcards`,
          {
            content: content,
            numberOfCards: numberOfCards.value
          },
          {
            headers: {
              'Authorization': `Bearer ${auth.getToken()}`
            }
          }
        )
        
        const flashcards = aiResponse.data.flashcards
        
        if (flashcards && flashcards.length > 0) {
          const savePromises = flashcards.map(flashcard =>
            axios.post(`${API_BASE_URL}/api/docs/flashcards`,
              {
                pageId: documentId.value,
                question: flashcard.question,
                answer: flashcard.answer,
                tags: []
              },
              {
                headers: {
                  'Authorization': `Bearer ${auth.getToken()}`,
                  'X-User-Id': auth.getUserId(),
                  'Content-Type': 'application/json'
                }
              }
            )
          )
          
          await Promise.all(savePromises)
          showSuccess.value = true
          
          setTimeout(() => {
            showSuccess.value = false
          }, 3000)
          
          await loadPageFlashcards()
        }
      } catch (error) {
        console.error('Failed to generate flashcards:', error)
        alert('Failed to generate flashcards. Please try again.')
      } finally {
        isGenerating.value = false
      }
    }

    async function loadPageFlashcards() {
      isLoadingFlashcards.value = true
      try {
        const response = await axios.get(
          `${API_BASE_URL}/api/docs/flashcards/page/${documentId.value}`,
          {
            headers: {
              'Authorization': `Bearer ${auth.getToken()}`,
              'X-User-Id': auth.getUserId()
            }
          }
        )
        pageFlashcards.value = response.data.data || []
      } catch (error) {
        console.error('Failed to load flashcards:', error)
        pageFlashcards.value = []
      } finally {
        isLoadingFlashcards.value = false
      }
    }

    async function toggleFlashcardsPanel() {
      showFlashcards.value = !showFlashcards.value
      if (showFlashcards.value && pageFlashcards.value.length === 0) {
        await loadPageFlashcards()
      }
    }

    async function deleteFlashcard(flashcardId) {
      if (!confirm('Are you sure you want to delete this flashcard?')) {
        return
      }
      
      try {
        await axios.delete(
          `${API_BASE_URL}/api/docs/flashcards/${flashcardId}`,
          {
            headers: {
              'Authorization': `Bearer ${auth.getToken()}`,
              'X-User-Id': auth.getUserId()
            }
          }
        )
        await loadPageFlashcards()
      } catch (error) {
        console.error('Failed to delete flashcard:', error)
        alert('Failed to delete flashcard')
      }
    }

    // Context files functions
    const loadUserFiles = async () => {
      const token = auth.getToken()
      if (!token) return

      try {
        const response = await axios.get(
          `${API_BASE_URL}/api/docs/files`,
          {
            headers: {
              'Authorization': `Bearer ${token}`
            }
          }
        )
        userFiles.value = response.data
      } catch (error) {
        console.error('Error loading files:', error)
      }
    }

    const uploadFile = async (event) => {
      const file = event.target.files[0]
      if (!file) return

      uploadError.value = null
      uploadSuccess.value = null
      isUploading.value = true
      currentUploadFile.value = file.name
      uploadAbortController.value = new AbortController()

      const token = auth.getToken()
      if (!token) {
        uploadError.value = 'Token not found. Please log in.'
        isUploading.value = false
        currentUploadFile.value = null
        uploadAbortController.value = null
        return
      }

      const formData = new FormData()
      formData.append('file', file)

      try {
        await axios.post(`${API_BASE_URL}/api/docs/files`, formData, {
          headers: {
            'Authorization': `Bearer ${token}`
          },
          signal: uploadAbortController.value.signal
        })

        uploadSuccess.value = `File "${file.name}" uploaded successfully!`
        await loadUserFiles()
        event.target.value = ''

        setTimeout(() => {
          uploadSuccess.value = null
        }, 3000)
      } catch (error) {
        if (error.name === 'CanceledError' || error.code === 'ERR_CANCELED') {
          uploadError.value = 'Upload canceled'
        } else if (error.response?.status === 409) {
          uploadError.value = `File "${file.name}" already exists`
        } else {
          uploadError.value = error.response?.data?.message || 'Error uploading file'
        }
      } finally {
        isUploading.value = false
        currentUploadFile.value = null
        uploadAbortController.value = null
      }
    }

    const deleteFile = async (fileId) => {
      try {
        const token = auth.getToken()
        if (!token) {
          uploadError.value = 'Token not found. Please log in.'
          return
        }

        await axios.delete(
          `${API_BASE_URL}/api/docs/files/${fileId}`,
          {
            headers: {
              'Authorization': `Bearer ${token}`
            }
          }
        )
        uploadSuccess.value = 'File deleted successfully'
        await loadUserFiles()
        selectedFiles.value = selectedFiles.value.filter(f => f.fileId !== fileId)
        
        setTimeout(() => {
          uploadSuccess.value = null
        }, 3000)
      } catch (error) {
        uploadError.value = error.response?.data?.error || error.response?.data?.message || 'Error deleting file'
      }
    }

    const toggleFileSelection = (file) => {
      const index = selectedFiles.value.findIndex(f => f.fileId === file.fileId)
      if (index === -1) {
        selectedFiles.value.push(file)
      } else {
        selectedFiles.value.splice(index, 1)
      }
    }

    const isFileSelected = (fileId) => {
      return selectedFiles.value.some(f => f.fileId === fileId)
    }

    const toggleContextFilesPanel = () => {
      showContextFiles.value = !showContextFiles.value
    }

    // Lifecycle hooks
    onMounted(async () => {
      // First, try to fetch existing HTML content for migration
      try {
        const token = auth.getToken()
        const response = await axios.get(
          `${API_BASE_URL}/api/docs/pages/${documentId.value}/content`,
          {
            headers: {
              'Authorization': `Bearer ${token}`,
              'X-User-Id': userId
            }
          }
        )
        
        if (response.data && response.data.length > 0) {
          initialContent.value = response.data
        }
      } catch (error) {
        // Continue anyway - document might be new or already migrated
      } finally {
        isLoadingContent.value = false
      }
      
      // Connect to Y.js for collaboration
      connect()
      
      // Load context files
      loadUserFiles()
    })

    onBeforeUnmount(() => {
      disconnect()
    })

    return {
      editor,
      ydoc,
      provider: computed(() => provider.value),
      user,
      isGenerating,
      isConnected,
      isSynced,
      hasError,
      errorMessage,
      showSuccess,
      numberOfCards,
      showFlashcards,
      pageFlashcards,
      isLoadingFlashcards,
      initialContent,
      isLoadingContent,
      handleEditorReady,
      generateFlashcards,
      toggleFlashcardsPanel,
      deleteFlashcard,
      documentId,
      showContextFiles,
      userFiles,
      selectedFiles,
      uploadError,
      uploadSuccess,
      isUploading,
      currentUploadFile,
      uploadFile,
      deleteFile,
      toggleFileSelection,
      isFileSelected,
      toggleContextFilesPanel,
      aiMode
    }
  }
}
</script>

<template>
  <div class="flex flex-row h-screen w-full bg-surface-light overflow-hidden">
    <div class="flex flex-col h-full w-full overflow-hidden">
      <!-- Top Header -->
      <div class="flex flex-row h-[50px] w-full border-b-2 border-accent flex-shrink-0 items-center px-4 justify-between">
        <div class="flex items-center gap-4">
          <h1 class="text-lg font-semibold text-content-primary">Document {{ documentId }}</h1>
          
          <!-- Connection Status -->
          <div class="flex items-center gap-2">
            <div 
              :class="[
                'w-2 h-2 rounded-full',
                isConnected ? 'bg-green-500' : 'bg-red-500'
              ]"
            ></div>
            <span class="text-sm text-content-secondary">
              {{ isConnected ? 'Connected' : 'Disconnected' }}
            </span>
          </div>
          
          <!-- Loading Status -->
          <div v-if="isLoadingContent" class="flex items-center gap-2 text-sm text-content-secondary">
            <div class="animate-spin w-3 h-3 border border-content-secondary border-t-transparent rounded-full"></div>
            <span>Loading...</span>
          </div>
          
          <!-- Syncing Status -->
          <div v-if="isConnected && !isSynced" class="flex items-center gap-2 text-sm text-orange-600">
            <div class="animate-pulse w-2 h-2 bg-orange-500 rounded-full"></div>
            <span>Syncing...</span>
          </div>
        </div>
        
        <!-- Flashcard Controls -->
        <div class="flex items-center gap-3">
          <!-- Number of cards input -->
          <div class="flex items-center gap-2">
            <label for="cardCount" class="text-sm text-content-primary">Cards:</label>
            <input 
              id="cardCount"
              v-model.number="numberOfCards" 
              type="number" 
              min="1" 
              max="20" 
              class="w-16 px-2 py-1 text-sm border border-border-opa rounded focus:outline-none focus:border-accent text-content-primary"
            />
          </div>
          
          <!-- Generate button -->
          <button
            @click="generateFlashcards"
            :disabled="isGenerating || !isConnected"
            class="px-4 py-1.5 text-sm font-medium text-surface-light bg-accent hover:bg-[#E66900] rounded transition-colors disabled:bg-content-secondary disabled:cursor-not-allowed flex items-center gap-2"
          >
            <span v-if="isGenerating" class="animate-spin w-3 h-3 border border-white border-t-transparent rounded-full"></span>
            <span>{{ isGenerating ? 'Generating...' : 'Generate Flashcards' }}</span>
          </button>
          
          <!-- Success message -->
          <transition name="fade">
            <span v-if="showSuccess" class="text-sm text-green-600 font-medium animate-fade-in">
              ✓ Flashcards saved!
            </span>
          </transition>
          
          <!-- Toggle flashcards panel button -->
          <button
            @click="toggleFlashcardsPanel"
            class="px-4 py-1.5 text-sm font-medium text-content-primary bg-surface-light-secondary hover:bg-surface-light rounded transition-colors flex items-center gap-2 border border-border-light"
          >
            <span>{{ showFlashcards ? 'Hide' : 'Show' }} Flashcards</span>
          </button>
          
          <!-- Toggle context files panel button -->
          <button
            @click="toggleContextFilesPanel"
            class="px-4 py-1.5 text-sm font-medium text-content-primary bg-surface-light-secondary hover:bg-surface-light rounded transition-colors flex items-center gap-2 border border-border-light"
          >
            <span>{{ showContextFiles ? 'Hide' : 'Show' }} Context Files</span>
          </button>
        </div>
      </div>
      
      <!-- Document Content Area -->
      <div class="flex flex-row w-full flex-1 relative overflow-auto">
        <!-- Error Overlay (Full Screen) -->
        <div 
          v-if="hasError"
          class="absolute inset-0 bg-surface-light z-50 flex items-center justify-center"
        >
          <div class="text-center">
            <svg class="mx-auto h-12 w-12 text-content-secondary mb-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
            </svg>
            <h2 class="text-2xl font-semibold text-content-primary mb-2">{{ errorMessage }}</h2>
            <p class="text-content-secondary">The document you're looking for doesn't exist or you don't have access to it.</p>
          </div>
        </div>
        
        <!-- Editor Container -->
        <div 
          class="flex flex-col flex-1 h-full w-full overflow-auto transition-all duration-300 ease-out"
          :class="{ 'mr-80': showFlashcards, 'ml-80': showContextFiles }"
        >
          <TiptapEditorYjs
            v-if="!hasError && provider && ydoc && !isLoadingContent"
            :ydoc="ydoc"
            :provider="provider"
            :user="user"
            :initial-content="initialContent"
            :selected-files="selectedFiles"
            :ai-mode="aiMode"
            @ready="handleEditorReady"
            placeholder="Start writing your document..."
          />
          <div v-else-if="!hasError" class="flex items-center justify-center h-full">
            <div class="text-content-secondary">Loading document content...</div>
          </div>
        </div>
        
        <!-- Flashcards Panel -->
        <FlashcardsPanel
          :show="showFlashcards && !hasError"
          :flashcards="pageFlashcards"
          :is-loading="isLoadingFlashcards"
          @close="toggleFlashcardsPanel"
          @delete="deleteFlashcard"
        />
        
        <!-- Context Files Panel -->
        <ContextFilesPanel
          :show="showContextFiles && !hasError"
          :files="userFiles"
          :selected-files="selectedFiles"
          :upload-error="uploadError"
          :upload-success="uploadSuccess"
          :is-uploading="isUploading"
          :current-upload-file="currentUploadFile"
          :mode="aiMode"
          @close="toggleContextFilesPanel"
          @upload="uploadFile"
          @delete="deleteFile"
          @toggle-selection="toggleFileSelection"
          @update:mode="aiMode = $event"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(-10px); }
  to { opacity: 1; transform: translateY(0); }
}
.animate-fade-in { animation: fadeIn 0.3s ease-out; }
.fade-enter-active, .fade-leave-active { transition: opacity 0.3s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
