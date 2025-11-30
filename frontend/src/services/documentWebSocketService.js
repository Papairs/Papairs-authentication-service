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
    if (this.webSocket) return

    this.webSocket = useWebSocket(this.url)
    this.webSocket.onOpen(() => this.callbacks.onConnected?.())
    this.webSocket.onClose(() => this.callbacks.onDisconnected?.())
    this.webSocket.onError((error) => this.callbacks.onError?.(error))
    this.webSocket.onMessage((event) => this._handleMessage(event))
  }

  /**
   * Connect to WebSocket server
   */
  connect() {
    if (!this.webSocket) return
    this.webSocket.connect()
  }

  /**
   * Disconnect from WebSocket server
   */
  disconnect() {
    this.webSocket?.disconnect()
  }

  /**
   * Join a document session
   * @param {string} docId - Document ID
   * @param {string} userId - User ID (required)
   */
  joinDocument(docId, userId) {
    if (!userId) {
      throw new Error('User ID is required')
    }
    return this.webSocket.send({ action: 'join', docId, userId })
  }

  /**
   * Send an operation to the server
   * @param {string} docId - Document ID
   * @param {object} operation - Operation to send
   * @param {string} userId - User ID (required)
   */
  sendOperation(docId, operation, userId) {
    if (!userId) {
      throw new Error('User ID is required')
    }
    return this.webSocket.send({ action: 'op', docId, op: operation, userId })
  }

  /**
   * Handle incoming WebSocket messages
   * @private
   */
  _handleMessage(event) {
    try {
      const message = JSON.parse(event.data)
      
      if (message.type === 'snapshot') {
        this.callbacks.onSnapshot?.(message)
      } else if (message.type === 'user_joined') {
        this.callbacks.onUserJoined?.(message)
      } else if (message.type === 'user_left') {
        this.callbacks.onUserLeft?.(message)
      } else if (message.clientId !== this.clientId) {
        this.callbacks.onOperation?.(message)
      }
    } catch (error) {
      this.callbacks.onError?.(error)
    }
  }

  /**
   * Get user ID from auth (authenticated users only)
   * @throws {Error} If user is not authenticated
   */
  getUserId() {
    const userId = auth.getUserId()
    if (!userId) {
      throw new Error('You do not have permission to be here. Please log in.')
    }
    return userId
  }

  /**
   * Register callbacks
   */
  on(event, callback) {
    if (Object.prototype.hasOwnProperty.call(this.callbacks, event)) {
      this.callbacks[event] = callback
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
