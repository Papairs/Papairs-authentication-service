<script>
import TiptapEditorYjs from '@/components/TiptapEditorYjs.vue'
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import TrashIcon from '@/components/icons/TrashIcon.vue'
import { useYjsDocument } from '@/composables/useYjsDocument'
import { API_BASE_URL } from '@/config'
import auth from '@/utils/auth'
import axios from 'axios'

export default {
  name: 'DocsView',
  components: { 
    TiptapEditorYjs,
    TrashIcon
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
      console.log('Editor ready')
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

    // Lifecycle hooks
    onMounted(async () => {
      console.log('\n🌐 [DocsView] Mounting document view for:', documentId.value)
      
      // First, try to fetch existing HTML content for migration
      try {
        const token = auth.getToken()
        console.log('📡 [DocsView] Fetching HTML content for potential migration...')
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
          console.log(`✅ [DocsView] HTML content available (${response.data.length} chars) - will pass to editor`)
          initialContent.value = response.data
        } else {
          console.log('📭 [DocsView] No HTML content - starting with empty document')
        }
      } catch (error) {
        console.log('⚠️  [DocsView] Could not fetch HTML:', error.message)
        // Continue anyway - document might be new or already migrated
      } finally {
        isLoadingContent.value = false
      }
      
      // Connect to Y.js for collaboration
      connect()
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
      documentId
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
          :class="{ 'mr-80': showFlashcards }"
        >
          <TiptapEditorYjs
            v-if="!hasError && provider && ydoc && !isLoadingContent"
            :ydoc="ydoc"
            :provider="provider"
            :user="user"
            :initial-content="initialContent"
            @ready="handleEditorReady"
            placeholder="Start writing your document..."
          />
          <div v-else-if="!hasError" class="flex items-center justify-center h-full">
            <div class="text-content-secondary">Loading document content...</div>
          </div>
        </div>
        
        <!-- Flashcards Panel -->
        <transition name="slide">
          <div v-if="showFlashcards && !hasError" class="absolute right-0 top-0 h-full w-80 bg-surface-light border-l-2 border-border-opa overflow-y-auto">
            <div class="sticky top-0 bg-surface-light border-b border-border-opa p-4 flex justify-between items-center">
              <h3 class="font-semibold text-content-primary">Page Flashcards</h3>
              <button @click="toggleFlashcardsPanel" class="text-content-secondary hover:text-content-primary transition-colors">✕</button>
            </div>
            <div v-if="isLoadingFlashcards" class="p-6 text-center text-content-secondary">Loading flashcards...</div>
            <div v-else-if="pageFlashcards.length === 0" class="p-6 text-center text-content-secondary">
              <p class="mb-2">No flashcards yet</p>
              <p class="text-sm">Generate some flashcards from your document!</p>
            </div>
            <div v-else class="p-4 space-y-3">
              <div v-for="card in pageFlashcards" :key="card.flashcardId" class="bg-surface-light-secondary border border-border-light rounded-lg p-4 hover:shadow-md transition-shadow">
                <div class="flex justify-end mb-2">
                  <button @click="deleteFlashcard(card.flashcardId)" class="text-xs text-content-secondary hover:text-red-600 transition-colors" title="Delete flashcard">
                    <TrashIcon :size="16" />
                  </button>
                </div>
                <div class="mb-3">
                  <p class="text-xs text-content-secondary font-medium mb-1">Question:</p>
                  <p class="text-xs text-content-primary">{{ card.question }}</p>
                </div>
                <div>
                  <p class="text-xs text-content-secondary font-medium mb-1">Answer:</p>
                  <p class="text-xs text-content-primary">{{ card.answer }}</p>
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
