<script>
import SidebarBase from '@/components/SidebarBase.vue'
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useWebSocket } from '@/composables/useWebSocket'
import { useTiptapDocument } from '@/composables/useTiptapDocument'
import { createErrorHandler } from '@/utils/errorHandler'
import { useRouter } from 'vue-router'
import auth from '@/utils/auth'
import axios from 'axios'

export default {
  name: 'DocsView',
  components: { 
    SidebarBase
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
    const document = useTiptapDocument(clientId, documentId.value)
    const webSocket = useWebSocket('ws://localhost:8082/ws/doc')
    
    const textarea = ref(null)
    const isGenerating = ref(false)
    const showSuccess = ref(false)
    const numberOfCards = ref(5)
    const showFlashcards = ref(false)
    const pageFlashcards = ref([])
    const isLoadingFlashcards = ref(false)

    webSocket.onOpen(() => {
      errorHandler.safe(() => {
        const userId = auth.getUserId() || 'anonymous'
        webSocket.send({ action: 'join', docId: documentId.value, userId: userId })
        console.log('[Application] Joined document session:', documentId.value, 'as user:', userId)
      })
    })

    webSocket.onMessage((event) => {
      errorHandler.safe(() => {
        const message = JSON.parse(event.data)
        console.log('[Application] Message received:', message.type)
        const success = message.type === 'snapshot' ? document.handleSnapshot(message) : document.handleServerOperation(message)
        if (!success) {
          errorHandler.document('Failed to process server message')
        }
      })
    })

    webSocket.onError((error) => {
      errorHandler.websocket('Connection error', error)
    })

    webSocket.onClose((event) => {
      console.log('[Application] WebSocket closed:', event.code)
    })

    function onInput(event) {
      errorHandler.safe(() => {
        const textValue = event.target.value
        // Convert plain text to simple HTML paragraph
        const htmlValue = '<p>' + textValue.replace(/\n/g, '</p><p>') + '</p>'
        const operation = document.handleHTMLInput(htmlValue)
        if (operation) {
          const userId = auth.getUserId() || 'anonymous'
          const messageWithUser = { action: 'op', docId: documentId.value, op: operation, userId: userId }
          if (!webSocket.send(messageWithUser)) {
            errorHandler.websocket('Failed to send operation to server')
          }
        }
      })
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

    onMounted(() => {
      console.log('[Application] Initializing document editor')
      webSocket.connect()
    })

    onBeforeUnmount(() => {
      console.log('[Application] Cleaning up document editor')
      webSocket.disconnect()
    })

    return {
      textarea, isGenerating, showSuccess, numberOfCards, showFlashcards, pageFlashcards, isLoadingFlashcards,
      text: document.text, version: document.version, hasPendingOperations: document.hasPendingOperations,
      connectionState: webSocket.connectionState, connectionError: webSocket.connectionError,
      documentId, onInput, generateFlashcards, toggleFlashcardsPanel, deleteFlashcard
    }
  }
}
</script>

<template>
  <div class="flex flex-row h-screen w-screen bg-surface-light overflow-hidden">
    <SidebarBase />
    <div class="flex flex-col h-full w-full overflow-hidden">
      <div class="flex flex-row h-[50px] w-full border-b-2 border-accent flex-shrink-0 items-center px-6 justify-between">
        <div class="text-content-primary font-medium">Document Editor</div>
        <div class="flex items-center gap-4">
          <div class="flex items-center gap-2">
            <label for="card-count" class="text-sm text-content-secondary">Cards:</label>
            <select id="card-count" v-model="numberOfCards" class="px-3 py-1 border border-border-light-subtle rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-accent">
              <option :value="3">3</option>
              <option :value="5">5</option>
              <option :value="7">7</option>
              <option :value="10">10</option>
              <option :value="15">15</option>
            </select>
          </div>
          <button @click="toggleFlashcardsPanel" class="px-4 py-2 bg-surface-light border border-border-light-subtle text-content-primary rounded-md font-medium hover:bg-gray-100 transition-colors flex items-center gap-2">
            <span>{{ showFlashcards ? '📚 Hide' : '📚 View' }} Flashcards</span>
            <span v-if="pageFlashcards.length > 0" class="text-xs bg-accent text-white px-2 py-0.5 rounded-full">{{ pageFlashcards.length }}</span>
          </button>
          <button @click="generateFlashcards" :disabled="isGenerating || !text || text.length < 50" class="px-4 py-2 bg-accent text-white rounded-md font-medium hover:opacity-90 disabled:opacity-50 disabled:cursor-not-allowed transition-opacity flex items-center gap-2">
            <span v-if="!isGenerating">🎴 Generate Flashcards</span>
            <span v-else>✨ Generating...</span>
          </button>
        </div>
        <div v-if="showSuccess" class="fixed top-4 right-4 bg-green-500 text-white px-6 py-3 rounded-lg shadow-lg z-50 animate-fade-in">✅ Flashcards created successfully!</div>
      </div>
      <div class="flex flex-col flex-1 w-full justify-center items-center overflow-hidden relative">
        <div class="flex flex-row h-[50px] w-[1200px] border-b border-border-light-subtle flex-shrink-0"></div>
        <div class="flex flex-row flex-1 relative w-full">
          <div class="flex-1 w-[1000px] bg-white border-x border-border-light-subtle px-12 overflow-hidden">
            <textarea ref="textarea" :value="text" @input="onInput" name="document-content" id="document-editor" placeholder="Start writing your document..." class="w-full h-full resize-none border-none focus:outline-none bg-transparent text-gray-800 text-base font-normal placeholder-gray-400 py-12"></textarea>
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
</style>
