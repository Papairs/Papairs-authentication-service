import { Server } from '@hocuspocus/server'
import { Logger } from '@hocuspocus/extension-logger'
import axios from 'axios'
import * as Y from 'yjs'

const DOCS_SERVICE_URL = process.env.DOCS_SERVICE_URL || 'http://docs-service:8082'
const ORCHESTRATION_URL = process.env.ORCHESTRATION_URL || 'http://orchestration-service:8080'
const PORT = process.env.PORT || 8083

// Track active connections per document
const documentConnections = new Map()

/**
 * Y.js Collaboration Service using Hocuspocus
 * Handles real-time collaborative editing with CRDT conflict resolution
 */
const server = Server.configure({
  port: PORT,
  
  // Keep documents in memory while users are connected
  timeout: 30000, // Keep document in memory for 30 seconds after last user disconnects
  debounce: 5000, // Debounce document saves (wait 5 seconds after last change)
  maxDebounce: 30000, // Maximum time to wait before forcing a save
  
  extensions: [
    new Logger(),
  ],

  /**
   * Authenticate users before allowing document access
   */
  async onAuthenticate({ token, documentName }) {
    if (!token) {
      throw new Error('Authentication required')
    }

    try {
      // Validate via orchestration gateway - it will validate token and add X-User-Id
      const response = await axios.get(
        `${ORCHESTRATION_URL}/api/docs/pages/${documentName}/yjs/validate`,
        {
          headers: { 
            'Authorization': `Bearer ${token}`,
            'X-Internal-Service': 'collaboration-service'
          }
        }
      )

      if (response.status === 200 && response.data?.userId) {
        const userId = response.data.userId
        return {
          userId,
          documentName
        }
      }
      
      throw new Error('Authentication failed')
    } catch (error) {
      console.error('Authentication error:', error.message)
      if (error.response?.status === 403) {
        throw new Error('Access denied')
      }
      if (error.response?.status === 401) {
        throw new Error('Invalid token')
      }
      throw new Error('Authentication failed')
    }
  },

  async onLoadDocument({ documentName }) {
    try {
      const response = await axios.get(
        `${DOCS_SERVICE_URL}/api/docs/pages/${documentName}/yjs`,
        {
          headers: { 'X-Internal-Service': 'collaboration-service' },
          responseType: 'arraybuffer'
        }
      )

      let stateArray = null
      
      if (response.data instanceof ArrayBuffer) {
        stateArray = new Uint8Array(response.data)
      } else if (Buffer.isBuffer(response.data)) {
        stateArray = new Uint8Array(response.data)
      } else if (Array.isArray(response.data)) {
        stateArray = new Uint8Array(response.data)
      } else if (response.data) {
        stateArray = new Uint8Array(Object.values(response.data))
      }
      
      if (stateArray && stateArray.length > 0) {
        const testDoc = new Y.Doc()
        Y.applyUpdate(testDoc, stateArray)
        const defaultFragment = testDoc.getXmlFragment('default')
        
        if (defaultFragment.toString().length > 0) {
          return stateArray
        }
      }
    } catch (error) {
      console.error(`Error loading document ${documentName}:`, error.message)
    }

    return null
  },

  async onStoreDocument({ documentName, document, context }) {
    if (!context?.userId) {
      return
    }

    try {
      const state = Y.encodeStateAsUpdate(document)
      
      await axios.post(
        `${DOCS_SERVICE_URL}/api/docs/pages/${documentName}/yjs`,
        {
          ydoc: Array.from(state)
        },
        {
          headers: {
            'X-User-Id': context.userId,
            'X-Internal-Service': 'collaboration-service',
            'Content-Type': 'application/json'
          }
        }
      )
    } catch (error) {
      console.error(`Failed to save ${documentName}:`, error.message)
      throw error
    }
  },

  async onConnect({ documentName }) {
    const currentCount = documentConnections.get(documentName) || 0
    documentConnections.set(documentName, currentCount + 1)
  },

  async onDisconnect({ documentName }) {
    const currentCount = documentConnections.get(documentName) || 1
    const newCount = Math.max(0, currentCount - 1)
    
    if (newCount === 0) {
      documentConnections.delete(documentName)
    } else {
      documentConnections.set(documentName, newCount)
    }
  },
  
  async afterLoadDocument({ documentName, document }) {
    const fragment = document.getXmlFragment('default')
    
    if (fragment.toString().length === 0) {
      try {
        const response = await axios.get(
          `${DOCS_SERVICE_URL}/api/docs/pages/${documentName}/yjs`,
          {
            headers: { 'X-Internal-Service': 'collaboration-service' },
            responseType: 'arraybuffer'
          }
        )
        
        if (response.data && (response.data.byteLength > 0 || response.data.length > 0)) {
          const stateArray = Buffer.isBuffer(response.data) 
            ? new Uint8Array(response.data) 
            : new Uint8Array(response.data)
          
          Y.applyUpdate(document, stateArray)
          return document
        }
      } catch (err) {
        console.error(`Failed to load state for ${documentName}:`, err.message)
      }
    }
  },
})

server.listen().then(() => {
})
