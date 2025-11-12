<script>
import SidebarBase from '@/components/SidebarBase.vue'
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useWebSocket } from '@/composables/useWebSocket'
import { useDocument } from '@/composables/useDocument'
import { createErrorHandler } from '@/utils/errorHandler'
import { useRouter } from 'vue-router'
import auth from '@/utils/auth'
import axios from 'axios'

export default {
  name: 'DocsView',
  components: { SidebarBase },
  props: {
    id: {
      type: String,
      default: 'demo'
    }
  },
  setup(props) {
    // Initialize error handler and client ID
    const errorHandler = createErrorHandler()
    const clientId = crypto.randomUUID()
    const router = useRouter()
    
    // Use document ID from props or default to 'demo'
    const documentId = computed(() => props.id)
    
    // Initialize document and WebSocket
    const document = useDocument(clientId)
    const webSocket = useWebSocket('ws://localhost:8082/ws/doc')
    
    // UI refs
    const textarea = ref(null)
    const isGenerating = ref(false)
    const showSuccess = ref(false)

    // WebSocket event handlers
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
        
        const success = message.type === 'snapshot' 
          ? document.handleSnapshot(message)
          : document.handleServerOperation(message)
          
        if (success) {
          updateTextarea()
        } else {
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

    // Text input handling
    function onInput(event) {
      errorHandler.safe(() => {
        const operation = document.handleTextInput(event.target.value)
        if (operation) {
          // Add user ID to the operation
          const userId = auth.getUserId() || 'anonymous'
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
        updateTextarea()
      })
    }

    // Update textarea value to match document state
    function updateTextarea() {
      if (textarea.value && textarea.value.value !== document.text.value) {
        textarea.value.value = document.text.value
      }
    }

    // Generate flashcards from current document content
    async function generateFlashcards() {
      const content = document.text.value.trim()
      
      if (!content || content.length < 50) {
        alert('Please write at least 50 characters before generating flashcards.')
        return
      }

      isGenerating.value = true
      showSuccess.value = false

      try {
        // Call AI service to generate flashcards
        const aiResponse = await axios.post('http://localhost:3001/generate-flashcards', {
          content: content,
          numberOfCards: 5
        })

        const flashcards = aiResponse.data.flashcards

        if (!flashcards || flashcards.length === 0) {
          throw new Error('No flashcards generated')
        }

        // Save flashcards to backend
        const headers = await auth.getAuthHeaders(router)
        
        for (const card of flashcards) {
          await axios.post('http://localhost:8082/api/docs/flashcards', {
            pageId: documentId.value,
            question: card.question,
            answer: card.answer,
            difficultyLevel: 'MEDIUM',
            tags: []
          }, {
            headers: {
              ...headers,
              'Content-Type': 'application/json'
            }
          })
        }

        showSuccess.value = true
        setTimeout(() => {
          showSuccess.value = false
        }, 3000)

        console.log(`Successfully generated and saved ${flashcards.length} flashcards`)
      } catch (error) {
        console.error('Failed to generate flashcards:', error)
        alert('Failed to generate flashcards. Please try again.')
      } finally {
        isGenerating.value = false
      }
    }

    // Lifecycle hooks
    onMounted(() => {
      console.log('[Application] Initializing document editor')
      webSocket.connect()
    })

    onBeforeUnmount(() => {
      console.log('[Application] Cleaning up document editor')
      webSocket.disconnect()
    })

    return {
      // UI refs
      textarea,
      isGenerating,
      showSuccess,
      
      // Document state
      text: document.text,
      version: document.version,
      hasPendingOperations: document.hasPendingOperations,
      
      // WebSocket state
      connectionState: webSocket.connectionState,
      connectionError: webSocket.connectionError,
      
      // Document info
      documentId,
      
      // Event handlers
      onInput,
      generateFlashcards
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
        
        <!-- Generate Flashcards Button -->
        <button
          @click="generateFlashcards"
          :disabled="isGenerating || !text || text.length < 50"
          class="px-4 py-2 bg-accent text-white rounded-md font-medium hover:opacity-90 disabled:opacity-50 disabled:cursor-not-allowed transition-opacity flex items-center gap-2"
        >
          <span v-if="!isGenerating">🎴 Generate Flashcards</span>
          <span v-else>✨ Generating...</span>
        </button>
        
        <!-- Success Message -->
        <div
          v-if="showSuccess"
          class="fixed top-4 right-4 bg-green-500 text-white px-6 py-3 rounded-lg shadow-lg z-50 animate-fade-in"
        >
          ✅ Flashcards created! Check the Flashcards page.
        </div>
      </div>
      <div class="flex flex-col flex-1 w-full justify-center items-center overflow-hidden">
        <div class="flex flex-row h-[50px] w-[1200px] border-b border-border-light-subtle flex-shrink-0"></div>
        <div class="flex flex-row flex-1">
          <div class="flex-1 w-[1000px] bg-white border-x border-border-light-subtle px-12 overflow-hidden">
            <textarea
              ref="textarea"
              :value="text"
              @input="onInput"
              name="document-content"
              id="document-editor"
              placeholder="Start writing your document..."
              class="w-full h-full resize-none border-none focus:outline-none bg-transparent text-gray-800 text-base font-normal placeholder-gray-400 py-12"
            ></textarea>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.animate-fade-in {
  animation: fadeIn 0.3s ease-out;
}
</style>