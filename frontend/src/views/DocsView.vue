<script>
import SidebarBase from '@/components/SidebarBase.vue'
import TiptapEditor from '@/components/TiptapEditor.vue'
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useWebSocket } from '@/composables/useWebSocket'
import { useTiptapDocument } from '@/composables/useTiptapDocument'
import { createErrorHandler } from '@/utils/errorHandler'
import auth from '@/utils/auth'

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
    // Initialize error handler and client ID
    const errorHandler = createErrorHandler()
    const clientId = crypto.randomUUID()
    
    // Use document ID from props or default to 'demo'
    const documentId = computed(() => props.id)
    
    // Initialize document and WebSocket
    const document = useTiptapDocument(clientId, documentId.value)
    const webSocket = useWebSocket('http://localhost:8082/ws/doc')
    
    // UI refs
    const editor = ref(null)
    const isConnected = ref(false)
    const isReceivingUpdate = ref(false)
    
    // Load initial content when document ID changes
    watch(documentId, async (newId) => {
      if (newId) {
        await document.loadContent(newId)
      }
    }, { immediate: true })

    // WebSocket event handlers
    webSocket.onOpen(() => {
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
    })

    webSocket.onMessage((event) => {
      errorHandler.safe(() => {
        const message = JSON.parse(event.data)
        
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

    // Lifecycle hooks
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
      
      // WebSocket state
      connectionState: webSocket.connectionState,
      connectionError: webSocket.connectionError,
      isConnected,
      
      // Document info
      documentId,
      
      // Event handlers
      handleContentChange,
      handleAutosave,
      handleEditorReady
    }
  }
}
</script>


<template>
  <div class="flex flex-row h-screen w-screen bg-surface-light overflow-hidden">
    <SidebarBase />
    <div class="flex flex-col h-full w-full overflow-hidden">
      <!-- Top Header -->
      <div class="flex flex-row h-[50px] w-full border-b-2 border-accent flex-shrink-0 items-center px-4">
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
        </div>
      </div>
    </div>
  </div>
</template>