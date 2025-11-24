import { ref, computed } from 'vue'
import { HtmlDocumentState } from '@/utils/htmlOperationalTransform'
import auth from '@/utils/auth'

/**
 * Enhanced document composable for Tiptap integration with collaborative editing
 * Uses HTML-aware operational transforms for better Tiptap compatibility
 */
export function useTiptapDocument(clientId, documentId) {
  // Create HTML-aware document state
  const documentState = new HtmlDocumentState(clientId)
  
  // Reactive state
  const htmlContent = ref('<p></p>')
  const isLoading = ref(false)
  const hasUnsavedChanges = ref(false)
  const lastSaveTime = ref(null)
  
  // Autosave state
  const isSaving = ref(false)
  const autosaveTimer = ref(null)
  const autosaveDelay = ref(5000) // 5 seconds
  
  // Computed properties for compatibility with existing code
  const text = computed(() => documentState.textContent)
  const version = computed(() => documentState.version)
  const hasPendingOperations = computed(() => documentState.pending.length > 0)
  
  /**
   * Handle snapshot from server - initialize with HTML content
   */
  const handleSnapshot = (snapshot) => {
    const success = documentState.handleSnapshot(snapshot)
    if (success) {
      htmlContent.value = documentState.htmlContent
      hasUnsavedChanges.value = false
      isLoading.value = false
    }
    return success
  }
  
  /**
   * Handle server operations for HTML content
   */
  const handleServerOperation = (op) => {
    const success = documentState.handleServerOperation(op)
    if (success) {
      htmlContent.value = documentState.htmlContent
    }
    return success
  }
  
  /**
   * Handle HTML content changes from Tiptap editor
   */
  const handleHTMLInput = (newHTML) => {
    htmlContent.value = newHTML
    hasUnsavedChanges.value = true
    
    const operation = documentState.handleHtmlChange(newHTML)
    
    // Schedule autosave
    scheduleAutosave()
    
    return operation
  }
  
  /**
   * Schedule autosave after user stops typing
   */
  const scheduleAutosave = () => {
    if (autosaveTimer.value) {
      clearTimeout(autosaveTimer.value)
    }
    
    autosaveTimer.value = setTimeout(() => {
      if (hasUnsavedChanges.value && !isSaving.value) {
        triggerAutosave()
      }
    }, autosaveDelay.value)
  }
  
  /**
   * Trigger immediate autosave
   */
  const triggerAutosave = async () => {
    if (!hasUnsavedChanges.value || isSaving.value) return
    
    isSaving.value = true
    
    try {
      // Save HTML content to backend
      const response = await saveToBackend(documentId, htmlContent.value)
      
      if (response.success) {
        hasUnsavedChanges.value = false
        lastSaveTime.value = new Date()
        console.log('[TiptapDocument] Autosave successful')
      } else {
        console.error('[TiptapDocument] Autosave failed:', response.error)
      }
    } catch (error) {
      console.error('[TiptapDocument] Autosave error:', error)
    } finally {
      isSaving.value = false
    }
  }
  
  /**
   * Save content to backend API
   */
  const saveToBackend = async (docId, content) => {
    try {
      console.log('[TiptapDocument] Saving to backend:', { docId, contentLength: content.length })
      
      // Get user ID from auth utility or generate temporary one
      let userId = auth.getUserId()
      if (!userId) {
        userId = 'temp-user-' + crypto.randomUUID().slice(0, 8)
        console.log('[TiptapDocument] Using temporary user ID:', userId)
      }
      
      const response = await fetch(`http://localhost:8082/api/docs/pages/${docId}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'X-User-Id': userId,
          'Accept': 'application/json'
        },
        body: JSON.stringify({ content })
      })
      
      console.log('[TiptapDocument] Save response status:', response.status)
      
      if (response.ok) {
        const result = await response.json()
        console.log('[TiptapDocument] Save successful:', result)
        return { success: true, data: result }
      } else {
        const errorText = await response.text()
        console.error('[TiptapDocument] Save failed:', response.status, errorText)
        return { 
          success: false, 
          error: `HTTP ${response.status}: ${errorText}` 
        }
      }
    } catch (error) {
      console.error('[TiptapDocument] Save error:', error)
      return { 
        success: false, 
        error: error.message 
      }
    }
  }
  
  /**
   * Load initial content from backend
   */
  const loadContent = async (docId) => {
    if (!docId) return
    
    isLoading.value = true
    
    try {
      console.log('[TiptapDocument] Loading content for document:', docId)
      
      // Get user ID from auth utility or generate temporary one
      let userId = auth.getUserId()
      if (!userId) {
        userId = 'temp-user-' + crypto.randomUUID().slice(0, 8)
        console.log('[TiptapDocument] Using temporary user ID:', userId)
      }
      
      const response = await fetch(`http://localhost:8082/api/docs/pages/${docId}`, {
        headers: {
          'X-User-Id': userId,
          'Accept': 'application/json'
        }
      })
      
      console.log('[TiptapDocument] Load response status:', response.status)
      
      if (response.ok) {
        const page = await response.json()
        const content = page.content || '<p></p>'
        
        console.log('[TiptapDocument] Content loaded:', { pageId: page.pageId, contentLength: content.length })
        
        // Update document state
        documentState.updateFromHtml(content)
        htmlContent.value = content
        hasUnsavedChanges.value = false
        
        console.log('[TiptapDocument] Content loaded successfully')
      } else if (response.status === 404) {
        // Page doesn't exist, try to create it
        console.log('[TiptapDocument] Page not found, attempting to create:', docId)
        const created = await createPage(docId, userId)
        if (created) {
          htmlContent.value = '<p></p>'
          documentState.updateFromHtml('<p></p>')
          hasUnsavedChanges.value = false
        } else {
          console.error('[TiptapDocument] Failed to create page')
          htmlContent.value = '<p></p>'
          documentState.updateFromHtml('<p></p>')
        }
      } else {
        const errorText = await response.text()
        console.error('[TiptapDocument] Failed to load content:', response.status, errorText)
        // Set default content on error
        htmlContent.value = '<p></p>'
        documentState.updateFromHtml('<p></p>')
      }
    } catch (error) {
      console.error('[TiptapDocument] Load content error:', error)
      // Set default content on error
      htmlContent.value = '<p></p>'
      documentState.updateFromHtml('<p></p>')
    } finally {
      isLoading.value = false
    }
  }
  
  /**
   * Create a new page if it doesn't exist
   */
  const createPage = async (docId, userId) => {
    try {
      console.log('[TiptapDocument] Creating new page:', docId)
      
      const response = await fetch(`http://localhost:8082/api/docs/pages`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'X-User-Id': userId,
          'Accept': 'application/json'
        },
        body: JSON.stringify({
          title: `Document ${docId}`,
          folderId: null // Root level
        })
      })
      
      if (response.ok) {
        const result = await response.json()
        console.log('[TiptapDocument] Page created successfully:', result.pageId)
        return true
      } else {
        const errorText = await response.text()
        console.error('[TiptapDocument] Page creation failed:', response.status, errorText)
        return false
      }
    } catch (error) {
      console.error('[TiptapDocument] Page creation error:', error)
      return false
    }
  }
  
  /**
   * Force save current content
   */
  const forceSave = async () => {
    if (autosaveTimer.value) {
      clearTimeout(autosaveTimer.value)
    }
    await triggerAutosave()
  }
  
  /**
   * Reset document state
   */
  const reset = () => {
    documentState.updateFromHtml('<p></p>')
    htmlContent.value = '<p></p>'
    hasUnsavedChanges.value = false
    lastSaveTime.value = null
    isLoading.value = false
    isSaving.value = false
    
    if (autosaveTimer.value) {
      clearTimeout(autosaveTimer.value)
    }
  }
  
  /**
   * Cleanup resources
   */
  const cleanup = () => {
    if (autosaveTimer.value) {
      clearTimeout(autosaveTimer.value)
    }
    
    // Force save before cleanup if there are unsaved changes
    if (hasUnsavedChanges.value) {
      triggerAutosave()
    }
  }
  
  return {
    // HTML-specific state
    htmlContent,
    isLoading,
    hasUnsavedChanges,
    lastSaveTime,
    isSaving,
    autosaveDelay,
    
    // Base document state (for WebSocket/OT compatibility)
    text,
    version,
    hasPendingOperations,
    
    // Methods
    handleSnapshot,
    handleServerOperation,
    handleHTMLInput,
    scheduleAutosave,
    triggerAutosave,
    forceSave,
    loadContent,
    reset,
    cleanup,
    
    // Debug helpers
    getDocumentState: () => ({
      html: htmlContent.value,
      text: documentState.textContent,
      version: documentState.version,
      pendingOps: documentState.pending.length,
      hasUnsavedChanges: hasUnsavedChanges.value,
      isSaving: isSaving.value
    })
  }
}