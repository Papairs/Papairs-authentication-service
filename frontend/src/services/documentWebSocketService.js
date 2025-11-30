import { useWebSocket } from '@/composables/useWebSocket'
import auth from '@/utils/auth'

/**
 * Document WebSocket Service
 * Manages WebSocket connection for real-time document collaboration
 */
export class DocumentWebSocketService {
  constructor(url = 'http://localhost:8082/ws/doc') {
    this.url = url
    this.webSocket = null
    this.clientId = crypto.randomUUID()
    this.callbacks = {
      onConnected: null,
      onDisconnected: null,
      onSnapshot: null,
      onOperation: null,
      onUserJoined: null,
      onUserLeft: null,
      onError: null
    }
  }

  /**
   * Initialize WebSocket connection
   */
  init() {
    if (this.webSocket) {
      console.warn('[DocumentWebSocketService] Already initialized')
      return
    }

    this.webSocket = useWebSocket(this.url)

    // Setup event handlers
    this.webSocket.onOpen(() => {
      console.log('[DocumentWebSocketService] Connected')
      this.callbacks.onConnected?.()
    })

    this.webSocket.onClose(() => {
      console.log('[DocumentWebSocketService] Disconnected')
      this.callbacks.onDisconnected?.()
    })

    this.webSocket.onError((error) => {
      console.error('[DocumentWebSocketService] Error:', error)
      this.callbacks.onError?.(error)
    })

    this.webSocket.onMessage((event) => {
      this._handleMessage(event)
    })
  }

  /**
   * Connect to WebSocket server
   */
  connect() {
    if (!this.webSocket) {
      console.error('[DocumentWebSocketService] Not initialized. Call init() first.')
      return
    }
    this.webSocket.connect()
  }

  /**
   * Disconnect from WebSocket server
   */
  disconnect() {
    if (this.webSocket) {
      this.webSocket.disconnect()
    }
  }

  /**
   * Join a document session
   * @param {string} docId - Document ID
   * @param {string} [userId] - Optional user ID (will use auth or generate temp)
   */
  joinDocument(docId, userId = null) {
    const user = userId || this._getUserId()
    const message = {
      action: 'join',
      docId,
      userId: user
    }
    
    if (this.webSocket.send(message)) {
      console.log('[DocumentWebSocketService] Joined document:', docId, 'as user:', user)
      return true
    }
    
    console.error('[DocumentWebSocketService] Failed to join document')
    return false
  }

  /**
   * Send an operation to the server
   * @param {string} docId - Document ID
   * @param {object} operation - Operation to send
   * @param {string} [userId] - Optional user ID
   */
  sendOperation(docId, operation, userId = null) {
    const user = userId || this._getUserId()
    const message = {
      action: 'op',
      docId,
      op: operation,
      userId: user
    }
    
    return this.webSocket.send(message)
  }

  /**
   * Handle incoming WebSocket messages
   * @private
   */
  _handleMessage(event) {
    try {
      const message = JSON.parse(event.data)
      
      switch (message.type) {
        case 'snapshot':
          this.callbacks.onSnapshot?.(message)
          break
          
        case 'user_joined':
          this.callbacks.onUserJoined?.(message)
          break
          
        case 'user_left':
          this.callbacks.onUserLeft?.(message)
          break
          
        default:
          // Handle operations from other clients
          if (message.clientId !== this.clientId) {
            this.callbacks.onOperation?.(message)
          }
          break
      }
    } catch (error) {
      console.error('[DocumentWebSocketService] Failed to parse message:', error)
      this.callbacks.onError?.(error)
    }
  }

  /**
   * Get user ID from auth or generate temporary one
   * @private
   */
  _getUserId() {
    let userId = auth.getUserId()
    if (!userId) {
      userId = 'temp-user-' + crypto.randomUUID().slice(0, 8)
      console.log('[DocumentWebSocketService] No authenticated user, using temporary ID:', userId)
    }
    return userId
  }

  /**
   * Register callbacks
   */
  on(event, callback) {
    if (Object.prototype.hasOwnProperty.call(this.callbacks, event)) {
      this.callbacks[event] = callback
    } else {
      console.warn(`[DocumentWebSocketService] Unknown event: ${event}`)
    }
  }

  /**
   * Get connection state
   */
  get connectionState() {
    return this.webSocket?.connectionState
  }

  /**
   * Get connection error
   */
  get connectionError() {
    return this.webSocket?.connectionError
  }

  /**
   * Get client ID
   */
  getClientId() {
    return this.clientId
  }
}

/**
 * Create a new document WebSocket service instance
 */
export function createDocumentWebSocketService(url) {
  return new DocumentWebSocketService(url)
}
