<script>
import SidebarBase from '@/components/SidebarBase.vue'
import TiptapEditor from '@/components/TiptapEditor.vue'
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useTiptapDocument } from '@/composables/useTiptapDocument'
import { createErrorHandler } from '@/utils/errorHandler'
import { createDocumentWebSocketService } from '@/services/documentWebSocketService'

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
    // Initialize error handler
    const errorHandler = createErrorHandler()
    
    // Use document ID from props or default to 'demo'
    const documentId = computed(() => props.id)
    
    // Initialize WebSocket service
    const wsService = createDocumentWebSocketService()
    wsService.init()
    
    // Initialize document manager
    const document = useTiptapDocument(wsService.getClientId())
    
    // UI refs
    const editor = ref(null)
    const isConnected = ref(false)
    const isReceivingUpdate = ref(false)

    // Helper function to update editor content
    const updateEditorContent = (newContent) => {
      if (!editor.value) return
      
      const currentContent = editor.value.getHTML()
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

    // Setup WebSocket event handlers
    wsService.on('onConnected', () => {
      errorHandler.safe(() => {
        wsService.joinDocument(documentId.value)
        isConnected.value = true
      })
    })

    wsService.on('onDisconnected', () => {
      isConnected.value = false
    })

    wsService.on('onSnapshot', (message) => {
      errorHandler.safe(() => {
        const success = document.handleSnapshot(message)
        if (success) {
          updateEditorContent(document.htmlContent.value)
        } else {
          errorHandler.document('Failed to process snapshot')
        }
      })
    })

    wsService.on('onOperation', (message) => {
      errorHandler.safe(() => {
        const success = document.handleServerOperation(message)
        if (success) {
          updateEditorContent(document.htmlContent.value)
        } else {
          errorHandler.document('Failed to process operation')
        }
      })
    })

    wsService.on('onUserJoined', (message) => {
      console.log('[DocsView] User joined:', message.userId)
    })

    wsService.on('onUserLeft', (message) => {
      console.log('[DocsView] User left:', message.userId)
    })

    wsService.on('onError', (error) => {
      errorHandler.websocket('WebSocket error', error)
    })

    // Handle content changes from Tiptap editor
    const handleContentChange = (html) => {
      if (isReceivingUpdate.value) {
        return
      }
      
      errorHandler.safe(() => {
        const operation = document.handleHTMLInput(html)
        
        if (operation && isConnected.value) {
          if (!wsService.sendOperation(documentId.value, operation)) {
            errorHandler.websocket('Failed to send operation to server')
          }
        }
      })
    }

    // Handle editor ready event
    const handleEditorReady = (editorInstance) => {
      editor.value = editorInstance
      
      if (document.htmlContent.value) {
        editorInstance.commands.setContent(document.htmlContent.value, false)
      }
    }

    // Lifecycle hooks
    onMounted(() => {
      wsService.connect()
    })

    onBeforeUnmount(() => {
      wsService.disconnect()
      document.cleanup()
    })

    return {
      // Document state
      htmlContent: document.htmlContent,
      version: document.version,
      hasPendingOperations: document.hasPendingOperations,
      isLoading: document.isLoading,
      
      // WebSocket state
      connectionState: wsService.connectionState,
      connectionError: wsService.connectionError,
      isConnected,
      
      // Document info
      documentId,
      
      // Event handlers
      handleContentChange,
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
      <div class="flex flex-col flex-1 h-full w-full overflow-auto">
        <TiptapEditor
          :model-value="htmlContent"
          @content-change="handleContentChange"
          @ready="handleEditorReady"
          placeholder="Start writing your document..."
        />
      </div>
    </div>
  </div>
</template>