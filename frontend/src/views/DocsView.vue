<script>
import TiptapEditor from '@/components/TiptapEditor.vue'
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useWebSocket } from '@/composables/useWebSocket'
import { useTiptapDocument } from '@/composables/useTiptapDocument'
import { createErrorHandler } from '@/utils/errorHandler'
import { useRouter } from 'vue-router'
import auth from '@/utils/auth'
import axios from 'axios'

export default {
  name: 'DocsView',
  components: { 
    TiptapEditor
  },
  props: {
    id: {
      type: String,
      default: 'demo'
    }
  },
  setup(props) {
    const errorHandler = createErrorHandler()
    const clientId = crypto.randomUUID()
    const router = useRouter()
    const documentId = computed(() => props.id)
    
    // Initialize document and WebSocket
    const document = useTiptapDocument(clientId, documentId.value)
    const webSocket = useWebSocket('http://localhost:8082/ws/doc')
    
    const editor = ref(null)
    const isGenerating = ref(false)    
    const isConnected = ref(false)
    const isReceivingUpdate = ref(false)
    
    // Load initial content when document ID changes
    watch(documentId, async (newId) => {
      if (newId) {
        await document.loadContent(newId)
      }
    }, { immediate: true })

    const showSuccess = ref(false)
    const numberOfCards = ref(5)
    const showFlashcards = ref(false)
    const pageFlashcards = ref([])
    const isLoadingFlashcards = ref(false)
    
    // Function to join a document
    const joinDocument = () => {
      errorHandler.safe(() => {
        // Get user ID from auth or generate a temporary one
        let userId = auth.getUserId()
        if (!userId) {
          userId = 'temp-user-' + crypto.randomUUID().slice(0, 8)
          console.log('[DocsView] No authenticated user, using temporary ID:', userId)
        }
        
        webSocket.send({ action: 'join', docId: documentId.value, userId: userId })
        isConnected.value = true
        console.log('[DocsView] Joined document session:', documentId.value, 'as user:', userId)
      })
    }

    // WebSocket event handlers
    webSocket.onOpen(() => {
      joinDocument()
    })

    webSocket.onMessage((event) => {
      errorHandler.safe(() => {
        const message = JSON.parse(event.data)
        console.log('[Application] Message received:', message.type, 'for doc:', message.docId || 'unknown')
        
        // Only process messages for the current document
        if (message.docId && message.docId !== documentId.value) {
          console.log('[Application] Ignoring message for different document')
          return
        }
        
        let success = false
        
        if (message.type === 'snapshot') {
          success = document.handleSnapshot(message)
          // Update Tiptap editor with snapshot content (only if different)
          if (success && editor.value) {
            const currentContent = editor.value.getHTML()
            const newContent = document.htmlContent.value
            if (currentContent !== newContent) {
              // Temporarily disable updates to prevent doubling
              isReceivingUpdate.value = true
              setTimeout(() => {
                editor.value.commands.setContent(newContent, false)
                setTimeout(() => {
                  isReceivingUpdate.value = false
                }, 100)
              }, 50)
            }
          }
        } else if (['user_joined', 'user_left'].includes(message.type)) {
          success = true
        } else {
          // Only apply operations from other users, not our own
          if (message.clientId !== clientId) {
            success = document.handleServerOperation(message)
            // Apply collaborative changes to Tiptap editor
            if (success && editor.value) {
              const currentContent = editor.value.getHTML()
              const newContent = document.htmlContent.value
              if (currentContent !== newContent) {
                isReceivingUpdate.value = true
                setTimeout(() => {
                  editor.value.commands.setContent(newContent, false)
                  setTimeout(() => {
                    isReceivingUpdate.value = false
                  }, 100)
                }, 50)
              }
            }
          } else {
            success = true
          }
        }
        
        if (!success && !['user_joined', 'user_left'].includes(message.type)) {
          errorHandler.document('Failed to process server message')
        }
      })
    })

    webSocket.onError((error) => {
      errorHandler.websocket('Connection error', error)
      isConnected.value = false
    })

    webSocket.onClose(() => {
      isConnected.value = false
    })

    // Watch for document ID changes
    watch(() => props.id, (newId, oldId) => {
      if (newId !== oldId) {
        console.log('[Application] Switching document from', oldId, 'to', newId)
        // Reset document state
        document.resetDocument()
        // Join new document if connected
        if (webSocket.connectionState.value === 'connected') {
          joinDocument()
        }
      }
    })

    // Handle content changes from Tiptap editor (for WebSocket collaboration)
    const handleContentChange = (html) => {
      if (isReceivingUpdate.value) {
        return
      }
      
      errorHandler.safe(() => {
        const operation = document.handleHTMLInput(html)
        
        if (operation && isConnected.value) {
          // Send operation to server for collaborative editing
          let userId = auth.getUserId()
          if (!userId) {
            userId = 'temp-user-' + crypto.randomUUID().slice(0, 8)
          }
          
          const messageWithUser = { 
            action: 'op', 
            docId: documentId.value, 
            op: operation,
            userId: userId
          }
          
          if (!webSocket.send(messageWithUser)) {
            errorHandler.websocket('Failed to send operation to server')
          }
        }
      })
    }
    

    // Handle autosave from Tiptap editor (different from collaborative changes)
    const handleAutosave = async (html) => {
      try {
        document.htmlContent.value = html
        await document.triggerAutosave()
      } catch (error) {
        errorHandler.document('Autosave failed', error)
      }
    }

    // Handle editor ready event
    const handleEditorReady = (editorInstance) => {
      editor.value = editorInstance
      
      if (document.htmlContent.value) {
        editorInstance.commands.setContent(document.htmlContent.value, false)
      }
    }

    async function generateFlashcards() {
      const content = document.text.value.trim()
      if (!content || content.length < 50) {
        alert('Please write at least 50 characters before generating flashcards.')
        return
      }
      isGenerating.value = true
      showSuccess.value = false
      try {
        const aiResponse = await axios.post('http://localhost:3001/generate-flashcards', {
          content: content,
          numberOfCards: numberOfCards.value
        })
        const flashcards = aiResponse.data.flashcards
        if (!flashcards || flashcards.length === 0) {
          throw new Error('No flashcards generated')
        }
        const headers = await auth.getAuthHeaders(router)
        for (const card of flashcards) {
          await axios.post('http://localhost:8082/api/docs/flashcards', {
            pageId: documentId.value,
            question: card.question,
            answer: card.answer,
            tags: []
          }, {
            headers: { ...headers, 'Content-Type': 'application/json' }
          })
        }
        showSuccess.value = true
        setTimeout(() => { showSuccess.value = false }, 3000)
        console.log('Successfully generated and saved ' + flashcards.length + ' flashcards')
        if (showFlashcards.value) {
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
        const headers = await auth.getAuthHeaders(router)
        const response = await axios.get('http://localhost:8082/api/docs/flashcards/page/' + documentId.value, { headers })
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
        const headers = await auth.getAuthHeaders(router)
        await axios.delete('http://localhost:8082/api/docs/flashcards/' + flashcardId, { headers })
        await loadPageFlashcards()
      } catch (error) {
        console.error('Failed to delete flashcard:', error)
        alert('Failed to delete flashcard. Please try again.')
      }
    }

    onMounted(async () => {
      if (documentId.value) {
        await document.loadContent(documentId.value)
      }
      webSocket.connect()
    })

    onBeforeUnmount(async () => {
      await document.forceSave()
      webSocket.disconnect()
      document.cleanup()
    })

    return {
      // Document state
      htmlContent: document.htmlContent,
      text: document.text,
      version: document.version,
      hasPendingOperations: document.hasPendingOperations,
      hasUnsavedChanges: document.hasUnsavedChanges,
      isSaving: document.isSaving,
      isLoading: document.isLoading,
      lastSaveTime: document.lastSaveTime,
      
      // WebSocket state
      connectionState: webSocket.connectionState,
      connectionError: webSocket.connectionError,
      isConnected,
      
      // Document info
      documentId,
      
      // Event handlers
      handleContentChange,
      handleAutosave,
      handleEditorReady,
      
      // Flashcard functionality
      isGenerating,
      showSuccess,
      numberOfCards,
      showFlashcards,
      pageFlashcards,
      isLoadingFlashcards,
      generateFlashcards,
      toggleFlashcardsPanel,
      deleteFlashcard
    }
  }
}

</script>

<template>
  <div class="flex flex-row h-screen w-screen bg-surface-light overflow-hidden">
    <div class="flex flex-col h-full w-full overflow-hidden">
      <!-- Top Header -->
      <div class="flex flex-row h-[50px] w-full border-b-2 border-accent flex-shrink-0 items-center px-4 justify-between">
        <div class="flex items-center gap-4">
          <h1 class="text-lg font-semibold text-gray-800">Document {{ documentId }}</h1>
          
          <!-- Connection Status -->
          <div class="flex items-center gap-2">
            <div 
              :class="[
                'w-2 h-2 rounded-full',
                isConnected ? 'bg-green-500' : 'bg-red-500'
              ]"
            ></div>
            <span class="text-sm text-gray-600">
              {{ isConnected ? 'Connected' : 'Disconnected' }}
            </span>
          </div>
          
          <!-- Loading Status -->
          <div v-if="isLoading" class="flex items-center gap-2 text-sm text-gray-600">
            <div class="animate-spin w-3 h-3 border border-gray-400 border-t-transparent rounded-full"></div>
            <span>Loading...</span>
          </div>
          
          <!-- Pending Operations -->
          <div v-if="hasPendingOperations" class="flex items-center gap-2 text-sm text-orange-600">
            <div class="animate-pulse w-2 h-2 bg-orange-500 rounded-full"></div>
            <span>Syncing...</span>
          </div>
        </div>
        
        <!-- Flashcard Controls -->
        <div class="flex items-center gap-3">
          <!-- Number of cards input -->
          <div class="flex items-center gap-2">
            <label for="cardCount" class="text-sm text-gray-600">Cards:</label>
            <input 
              id="cardCount"
              v-model.number="numberOfCards" 
              type="number" 
              min="1" 
              max="20" 
              class="w-16 px-2 py-1 text-sm border border-gray-300 rounded focus:outline-none focus:border-accent"
            />
          </div>
          
          <!-- Generate button -->
          <button
            @click="generateFlashcards"
            :disabled="isGenerating"
            class="px-4 py-1.5 text-sm font-medium text-white bg-accent hover:bg-accent-dark rounded transition-colors disabled:bg-gray-400 disabled:cursor-not-allowed flex items-center gap-2"
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
            class="px-4 py-1.5 text-sm font-medium text-gray-700 bg-gray-100 hover:bg-gray-200 rounded transition-colors flex items-center gap-2"
          >
            <span>📚</span>
            <span>{{ showFlashcards ? 'Hide' : 'Show' }} Flashcards</span>
          </button>
        </div>
      </div>
      
      <!-- Document Content Area -->
      <div class="flex flex-col flex-1 w-full overflow-auto">
        <div class="flex flex-row w-full justify-center min-h-full">
          <div class="w-full max-w-[1000px] min-w-[1000px] bg-white border-x border-b border-border-light-subtle h-fit min-h-full">
            <TiptapEditor
              :model-value="htmlContent"
              @content-change="handleContentChange"
              @autosave="handleAutosave"
              @ready="handleEditorReady"
              placeholder="Start writing your document..."
              :autosave-delay="5000"
            />
          </div>
          <transition name="slide">
            <div v-if="showFlashcards" class="absolute right-0 top-0 h-full w-96 bg-white border-l-2 border-accent shadow-xl overflow-y-auto z-10">
              <div class="sticky top-0 bg-white border-b border-border-light-subtle p-4 flex justify-between items-center">
                <h3 class="font-semibold text-content-primary">Page Flashcards</h3>
                <button @click="toggleFlashcardsPanel" class="text-content-secondary hover:text-content-primary transition-colors">✕</button>
              </div>
              <div v-if="isLoadingFlashcards" class="p-6 text-center text-content-secondary">Loading flashcards...</div>
              <div v-else-if="pageFlashcards.length === 0" class="p-6 text-center text-content-secondary">
                <p class="mb-2">📝 No flashcards yet</p>
                <p class="text-sm">Generate some flashcards from your document!</p>
              </div>
              <div v-else class="p-4 space-y-3">
                <div v-for="card in pageFlashcards" :key="card.flashcardId" class="bg-surface-light border border-border-light-subtle rounded-lg p-4 hover:shadow-md transition-shadow">
                  <div class="flex justify-end mb-2">
                    <button @click="deleteFlashcard(card.flashcardId)" class="text-xs text-content-secondary hover:text-red-600 transition-colors" title="Delete flashcard">🗑️</button>
                  </div>
                  <div class="mb-3">
                    <p class="text-xs text-content-secondary font-medium mb-1">Question:</p>
                    <p class="text-sm text-content-primary">{{ card.question }}</p>
                  </div>
                  <div>
                    <p class="text-xs text-content-secondary font-medium mb-1">Answer:</p>
                    <p class="text-sm text-content-primary">{{ card.answer }}</p>
                  </div>
                  <div v-if="card.tags && card.tags.length > 0" class="mt-3 flex flex-wrap gap-1">
                    <span v-for="tag in card.tags" :key="tag" class="text-xs px-2 py-1 bg-accent/10 text-accent rounded">{{ tag }}</span>
                  </div>
                </div>
              </div>
            </div>
          </transition>
        </div>
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
.slide-enter-active, .slide-leave-active { transition: transform 0.3s ease-out; }
.slide-enter-from { transform: translateX(100%); }
.slide-leave-to { transform: translateX(100%); }
.fade-enter-active, .fade-leave-active { transition: opacity 0.3s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
