import { ref } from 'vue'
import SockJS from 'sockjs-client'

export function useWebSocket(url, options = {}) {
  // State
  const ws = ref(null)
  const connecting = ref(false)
  const connectionState = ref('closed')
  const connectionError = ref(null)
  const messageQueue = []

  // Config
  const config = {
    initialDelay: options.initialReconnectDelay || 500,
    maxDelay: options.maxReconnectDelay || 30000
  }
  let reconnectDelay = config.initialDelay
  let reconnectTimer = null

  // Event handlers storage
  const handlers = {}

  // Smart connection checker
  const canConnect = () => {
    if (connecting.value) return false
    const state = ws.value?.readyState
    return state !== WebSocket.OPEN && state !== WebSocket.CONNECTING
  }

  // Message queue drainer
  const drainQueue = () => {
    if (ws.value?.readyState !== WebSocket.OPEN) return
    while (messageQueue.length > 0) {
      const data = messageQueue.shift()
      try {
        ws.value.send(typeof data === 'string' ? data : JSON.stringify(data))
      } catch (error) {
        messageQueue.unshift(data) // Put it back
        break
      }
    }
  }

  // Smart reconnect scheduler
  const scheduleReconnect = () => {
    if (reconnectTimer) return
    reconnectTimer = setTimeout(() => {
      reconnectTimer = null
      reconnectDelay = Math.min(reconnectDelay * 2, config.maxDelay)
      connect()
    }, reconnectDelay)
  }

  // Event handler caller - safe execution
  const callHandler = (event, ...args) => {
    try {
      handlers[event]?.(...args)
    } catch (error) {
      console.error(`[WebSocket] Error in ${event} handler:`, error.message)
    }
  }

  // Core connection function
  const connect = () => {
    if (!canConnect()) return

    connecting.value = true
    connectionState.value = 'connecting'
    connectionError.value = null

    try {
      // Use SockJS for better compatibility
      ws.value = new SockJS(url.replace('ws://', 'http://').replace('/ws/doc', '/ws/doc'))

      ws.value.onopen = () => {
        connecting.value = false
        connectionState.value = 'open'
        reconnectDelay = config.initialDelay
        callHandler('onOpen')
        drainQueue()
      }

      ws.value.onclose = (event) => {
        connectionState.value = 'closed'
        connecting.value = false
        if (event.code !== 1000) {
          connectionError.value = `Connection lost: ${event.reason || 'Unknown error'}`
          scheduleReconnect()
        }
        callHandler('onClose', event)
      }

      ws.value.onerror = (error) => {
        connectionError.value = 'Connection error occurred'
        callHandler('onError', error)
        try { 
          ws.value?.close() 
        } catch (closeError) {
          // Ignore close errors during error handling
        }
      }

      ws.value.onmessage = (event) => callHandler('onMessage', event)

    } catch (error) {
      console.error('[WebSocket] Failed to create connection:', error.message)
      connectionError.value = 'Failed to create WebSocket connection'
      connecting.value = false
      connectionState.value = 'closed'
      scheduleReconnect()
    }
  }

  // Smart send function
  const send = (data) => {
    const state = ws.value?.readyState
    
    if (state === WebSocket.OPEN) {
      try {
        ws.value.send(typeof data === 'string' ? data : JSON.stringify(data))
        return true
      } catch (error) {
        console.error('[WebSocket] Send failed:', error.message)
        connectionError.value = 'Failed to send message'
        return false
      }
    }
    
    // Queue message and try to reconnect if needed
    messageQueue.push(data)
    if (state !== WebSocket.CONNECTING) scheduleReconnect()
    return false
  }

  // Clean disconnect
  const disconnect = () => {
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }
    try { 
      ws.value?.close() 
    } catch (closeError) {
      // Ignore close errors during disconnect
    }
  }

  // Event handler setters - clean API
  const on = (event, handler) => { handlers[event] = handler }

  return {
    connectionState,
    connectionError,
    connecting,
    connect,
    disconnect,
    send,
    onOpen: (handler) => on('onOpen', handler),
    onClose: (handler) => on('onClose', handler),
    onError: (handler) => on('onError', handler),
    onMessage: (handler) => on('onMessage', handler)
  }
}