<script>
import SidebarBase from '@/components/SidebarBase.vue'
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
    SidebarBase,
    TiptapEditor
  },
  props: {
    id: {
      type: String,
      default: 'demo'
    }
  },
  setup(props) {
    // General / shared stuff
    const errorHandler = createErrorHandler()
    const clientId = crypto.randomUUID()
    const router = useRouter()

    const documentId = computed(() => props.id)

    // Tiptap document + collaborative editing
    const document = useTiptapDocument(clientId, documentId.value)
    const webSocket = useWebSocket('ws://localhost:8082/ws/doc')

    const editor = ref(null)
    const isConnected = ref(false)
    const isReceivingUpdate = ref(false)

    // FLASHCARDS UI state
    const isGenerating = ref(false)
    const showSuccess = ref(false)
    const numberOfCards = ref(5)
    const showFlashcards = ref(false)
    const pageFlashcards = ref([])
    const isLoadingFlashcards = ref(false)

    // Derived length of content (plain text) for enabling the Generate button
    const contentLength = computed(() => {
      if (editor.value) {
        return editor.value.getText().trim().length
      }
      const html = document.htmlContent.value || ''
      return html.replace(/<[^>]+>/g, '').trim().length
    })

    // Load initial content when document ID changes
    watch(
      documentId,
      async (newId) => {
        if (newId) {
          await document.loadContent(newId)
        }
      },
      { immediate: true }
    )

    // WebSocket event handlers
    webSocket.onOpen(() => {
      errorHandler.safe(() => {
        let userId = auth.getUserId()
        if (!userId) {
          userId = 'temp-user-' + crypto.randomUUID().slice(0, 8)
          console.log('[DocsView] No authenticated user, using temporary ID:', userId)
        }

        webSocket.send({ action: 'join', docId: documentId.value, userId })
        isConnected.value = true
        console.log('[DocsView] Joined document session:', documentId.value, 'as user:', userId)
      })
    })

    webSocket.onMessage((event) => {
      errorHandler.safe(() => {
        const message = JSON.parse(event.data)

        let success = false

        if (message.type === 'snapshot') {
          success = document.handleSnapshot(message)

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
        } else if (['user_joined', 'user_left'].includes(message.type)) {
          success = true
        } else {
          // Only apply operations from other clients
          if (message.clientId !== clientId) {
            success = document.handleServerOperation(message)

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

    // Handle content changes from Tiptap (collab ops)
    const handleContentChange = (html) => {
      if (isReceivingUpdate.value) return

      errorHandler.safe(() => {
        const operation = document.handleHTMLInput(html)

        if (operation && isConnected.value) {
          let userId = auth.getUserId()
          if (!userId) {
            userId = 'temp-user-' + crypto.randomUUID().slice(0, 8)
          }

          const messageWithUser = {
            action: 'op',
            docId: documentId.value,
            op: operation,
            userId
          }

          if (!webSocket.send(messageWithUser)) {
            errorHandler.websocket('Failed to send operation to server')
          }
        }
      })
    }

    // Autosave handler
    const handleAutosave = async (html) => {
      try {
        document.htmlContent.value = html
        await document.triggerAutosave()
      } catch (error) {
        errorHandler.document('Autosave failed', error)
      }
    }

    const handleEditorReady = (editorInstance) => {
      editor.value = editorInstance

      if (document.htmlContent.value) {
        editorInstance.commands.setContent(document.htmlContent.value, false)
      }
    }

    // FLASHCARDS: generate from current document content
    const generateFlashcards = async () => {
      // Prefer plain text from editor
      const content =
        editor.value?.getText().trim() ||
        document.htmlContent.value.replace(/<[^>]+>/g, '').trim()

      if (!content || content.length < 50) {
        alert('Please write at least 50 characters before generating flashcards.')
        return
      }

      isGenerating.value = true
      showSuccess.value = false

      try {
        // Call AI service to generate flashcards
        const aiResponse = await axios.post('http://localhost:3001/generate-flashcards', {
          content,
          numberOfCards: numberOfCards.value
        })

        const flashcards = aiResponse.data.flashcards

        if (!flashcards || flashcards.length === 0) {
          throw new Error('No flashcards generated')
        }

        // Save flashcards to backend
        const headers = await auth.getAuthHeaders(router)

        for (const card of flashcards) {
          await axios.post(
            'http://localhost:8082/api/docs/flashcards',
            {
              pageId: documentId.value,
              question: card.question,
              answer: card.answer,
              tags: []
            },
            {
              headers: {
                ...headers,
                'Content-Type': 'application/json'
              }
            }
          )
        }

        showSuccess.value = true
        setTimeout(() => {
          showSuccess.value = false
        }, 3000)

        console.log(`Successfully generated and saved ${flashcards.length} flashcards`)

        // Reload flashcards if the panel is open
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

    // Load flashcards for this page
    const loadPageFlashcards = async () => {
      isLoadingFlashcards.value = true
      try {
        const headers = await auth.getAuthHeaders(router)
        const response = await axios.get(
          `http://localhost:8082/api/docs/flashcards/page/${documentId.value}`,
          { headers }
        )
        pageFlashcards.value = response.data.data || []
      } catch (error) {
        console.error('Failed to load flashcards:', error)
        pageFlashcards.value = []
      } finally {
        isLoadingFlashcards.value = false
      }
    }

    // Toggle flashcards panel
    const toggleFlashcardsPanel = async () => {
      showFlashcards.value = !showFlashcards.value
      if (showFlashcards.value && pageFlashcards.value.length === 0) {
        await loadPageFlashcards()
      }
    }

    // Delete a flashcard
    const deleteFlashcard = async (flashcardId) => {
      if (!confirm('Are you sure you want to delete this flashcard?')) {
        return
      }

      try {
        const headers = await auth.getAuthHeaders(router)
        await axios.delete(`http://localhost:8082/api/docs/flashcards/${flashcardId}`, { headers })
        await loadPageFlashcards()
      } catch (error) {
        console.error('Failed to delete flashcard:', error)
        alert('Failed to delete flashcard. Please try again.')
      }
    }

    // Lifecycle
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
      version: document.version,
      hasPendingOperations: document.hasPendingOperations,
      hasUnsavedChanges: document.hasUnsavedChanges,
      isSaving: document.isSaving,
      isLoading: document.isLoading,
      lastSaveTime: document.lastSaveTime,

      // WebSocket / connection
      connectionState: webSocket.connectionState,
      connectionError: webSocket.connectionError,
      isConnected,

      // Document info
      documentId,

      // Editor + handlers
      handleContentChange,
      handleAutosave,
      handleEditorReady,

      // Flashcards UI
      isGenerating,
      showSuccess,
      numberOfCards,
      showFlashcards,
      pageFlashcards,
      isLoadingFlashcards,
      contentLength,

      // Flashcards handlers
      generateFlashcards,
      toggleFlashcardsPanel,
      deleteFlashcard
    }
  }
}
</script>

<template>
  <div class="flex flex-row h-screen w-screen bg-surface-light overflow-hidden">
    <SidebarBase />
    <div class="flex flex-col h-full w-full overflow-hidden">
      <!-- Top Header -->
      <div class="flex flex-row h-[50px] w-full border-b-2 border-accent flex-shrink-0 items-center px-4 justify-between">
        <!-- Left: document + connection status -->
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

        <!-- Right: Flashcards controls -->
        <div class="flex items-center gap-4">
          <!-- Number of Cards Selector -->
          <div class="flex items-center gap-2">
            <label for="card-count" class="text-sm text-content-secondary">Cards:</label>
            <select
              id="card-count"
              v-model="numberOfCards"
              class="px-3 py-1 border border-border-light-subtle rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-accent"
            >
              <option :value="3">3</option>
              <option :value="5">5</option>
              <option :value="7">7</option>
              <option :value="10">10</option>
              <option :value="15">15</option>
            </select>
          </div>

          <!-- View Flashcards Button -->
          <button
            @click="toggleFlashcardsPanel"
            class="px-4 py-2 bg-surface-light border border-border-light-subtle text-content-primary rounded-md font-medium hover:bg-gray-100 transition-colors flex items-center gap-2"
          >
            <span>{{ showFlashcards ? '📚 Hide' : '📚 View' }} Flashcards</span>
            <span v-if="pageFlashcards.length > 0" class="text-xs bg-accent text-white px-2 py-0.5 rounded-full">
              {{ pageFlashcards.length }}
            </span>
          </button>

          <!-- Generate Flashcards Button -->
          <button
            @click="generateFlashcards"
            :disabled="isGenerating || contentLength < 50"
            class="px-4 py-2 bg-accent text-white rounded-md font-medium hover:opacity-90 disabled:opacity-50 disabled:cursor-not-allowed transition-opacity flex items-center gap-2"
          >
            <span v-if="!isGenerating">🎴 Generate Flashcards</span>
            <span v-else>✨ Generating...</span>
          </button>
        </div>

        <!-- Success Toast -->
        <div
          v-if="showSuccess"
          class="fixed top-4 right-4 bg-green-500 text-white px-6 py-3 rounded-lg shadow-lg z-50 animate-fade-in"
        >
          ✅ Flashcards created successfully!
        </div>
      </div>

      <!-- Document + Flashcards area -->
      <div class="flex flex-col flex-1 w-full overflow-hidden">
        <div class="flex flex-row w-full justify-center min-h-full relative">
          <!-- Editor area -->
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

          <!-- Flashcards Side Panel -->
          <transition name="slide">
            <div
              v-if="showFlashcards"
              class="absolute right-0 top-0 h-full w-96 bg-white border-l-2 border-accent shadow-xl overflow-y-auto z-10"
            >
              <!-- Panel Header -->
              <div class="sticky top-0 bg-white border-b border-border-light-subtle p-4 flex justify-between items-center">
                <h3 class="font-semibold text-content-primary">Page Flashcards</h3>
                <button
                  @click="toggleFlashcardsPanel"
                  class="text-content-secondary hover:text-content-primary transition-colors"
                >
                  ✕
                </button>
              </div>

              <!-- Loading State -->
              <div v-if="isLoadingFlashcards" class="p-6 text-center text-content-secondary">
                Loading flashcards...
              </div>

              <!-- Empty State -->
              <div v-else-if="pageFlashcards.length === 0" class="p-6 text-center text-content-secondary">
                <p class="mb-2">📝 No flashcards yet</p>
                <p class="text-sm">Generate some flashcards from your document!</p>
              </div>

              <!-- Flashcards List -->
              <div v-else class="p-4 space-y-3">
                <div
                  v-for="card in pageFlashcards"
                  :key="card.flashcardId"
                  class="bg-surface-light border border-border-light-subtle rounded-lg p-4 hover:shadow-md transition-shadow"
                >
                  <!-- Card Header -->
                  <div class="flex justify-end mb-2">
                    <button
                      @click="deleteFlashcard(card.flashcardId)"
                      class="text-xs text-content-secondary hover:text-red-600 transition-colors"
                      title="Delete flashcard"
                    >
                      🗑️
                    </button>
                  </div>

                  <!-- Question -->
                  <div class="mb-3">
                    <p class="text-xs text-content-secondary font-medium mb-1">Question:</p>
                    <p class="text-sm text-content-primary whitespace-pre-wrap">
                      {{ card.question }}
                    </p>
                  </div>

                  <!-- Answer -->
                  <div>
                    <p class="text-xs text-content-secondary font-medium mb-1">Answer:</p>
                    <p class="text-sm text-content-primary whitespace-pre-wrap">
                      {{ card.answer }}
                    </p>
                  </div>

                  <!-- Tags -->
                  <div v-if="card.tags && card.tags.length > 0" class="mt-3 flex flex-wrap gap-1">
                    <span
                      v-for="tag in card.tags"
                      :key="tag"
                      class="text-xs px-2 py-1 bg-accent/10 text-accent rounded"
                    >
                      {{ tag }}
                    </span>
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

/* Slide transition for flashcards panel */
.slide-enter-active,
.slide-leave-active {
  transition: transform 0.3s ease-out;
}

.slide-enter-from {
  transform: translateX(100%);
}

.slide-leave-to {
  transform: translateX(100%);
}
</style>
