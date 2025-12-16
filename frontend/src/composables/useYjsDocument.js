import { ref, onBeforeUnmount } from 'vue'
import * as Y from 'yjs'
import { HocuspocusProvider } from '@hocuspocus/provider'
import { getCollaborationWebSocketUrl } from '@/config'
import auth from '@/utils/auth'

/**
 * Y.js-based collaborative document manager
 * Uses CRDT for conflict-free collaborative editing
 */
export function useYjsDocument(documentId) {
  const ydoc = new Y.Doc()
  const provider = ref(null)
  const isConnected = ref(false)
  const isSynced = ref(false)
  const error = ref(null)

  /**
   * Connect to Y.js collaboration service
   */
  function connect() {
    try {
      const token = auth.getToken()
      if (!token) {
        throw new Error('User not authenticated')
      }

      const wsUrl = getCollaborationWebSocketUrl()
      
      // Create Hocuspocus provider with our Y.Doc
      provider.value = new HocuspocusProvider({
        url: wsUrl,
        name: documentId,
        document: ydoc,
        token: token,
        onSynced: ({ state }) => {
          isSynced.value = state
        }
      })

      // Connection status handlers
      provider.value.on('status', ({ status }) => {
        console.log('Y.js connection status:', status)
        isConnected.value = status === 'connected'
        
        if (status === 'disconnected') {
          error.value = 'Disconnected from collaboration server'
        } else {
          error.value = null
        }
      })

      provider.value.on('sync', (synced) => {
        isSynced.value = synced
      })

      // Error handler
      provider.value.on('connection-error', (err) => {
        console.error('Y.js connection error:', err)
        error.value = 'Failed to connect to collaboration server'
      })

      provider.value.on('connection-close', (event) => {
        console.log('Y.js connection closed:', event)
        if (event.code === 1008) {
          error.value = 'Access denied'
        }
      })

    } catch (err) {
      console.error('Failed to initialize Y.js:', err)
      error.value = err.message
    }
  }

  /**
   * Disconnect from Y.js collaboration service
   */
  function disconnect() {
    if (provider.value) {
      provider.value.disconnect()
      provider.value.destroy()
      provider.value = null
    }
    ydoc.destroy()
  }

  /**
   * Cleanup on component unmount
   */
  onBeforeUnmount(() => {
    disconnect()
  })

  return {
    ydoc,
    provider,
    isConnected,
    isSynced,
    error,
    connect,
    disconnect
  }
}
