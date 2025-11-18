/**
 * HTML-based document state management for collaborative editing
 * Uses full HTML synchronization instead of text-based operational transforms
 */

/**
 * Enhanced document state for HTML content
 * Now works directly with HTML operations instead of text-based OT
 */
export class HtmlDocumentState {
  constructor(clientId) {
    this.clientId = clientId
    this.htmlContent = '<p></p>'
    this.version = 0
    this.pending = []
    this.lastHtml = '<p></p>'
  }

  /**
   * Update content from HTML input
   */
  updateFromHtml(newHtml) {
    this.htmlContent = newHtml || '<p></p>'
    this.lastHtml = this.htmlContent
  }

  /**
   * Handle snapshot from server
   */
  handleSnapshot(snapshot) {
    this.htmlContent = snapshot.content || '<p></p>'
    this.version = snapshot.version || 0
    this.pending = []
    this.lastHtml = this.htmlContent
    return true
  }

  /**
   * Handle server operation - now works with HTML directly
   */
  handleServerOperation(op) {
    if (!op || !op.htmlContent) {
      return false
    }

    try {
      const isOurs = this.pending[0]?.opId === op.opId && 
                     this.pending[0]?.clientId === op.clientId

      if (isOurs) {
        this.pending.shift()
        this.version = op.version || this.version + 1
        return true
      }

      this.htmlContent = op.htmlContent
      this.version = op.version || this.version + 1
      this.lastHtml = this.htmlContent
      
      return true
    } catch (error) {
      return false
    }
  }

  /**
   * Handle local HTML changes - now creates HTML-based operations
   */
  handleHtmlChange(newHtml) {
    newHtml = newHtml || '<p></p>'
    
    if (this.lastHtml === newHtml) {
      return null
    }
    
    this.htmlContent = newHtml

    const operation = {
      type: 'html-update',
      htmlContent: newHtml,
      version: this.version,
      clientId: this.clientId,
      opId: crypto.randomUUID(),
      baseVersion: this.version + this.pending.length,
      timestamp: Date.now()
    }

    this.pending.push(operation)
    this.lastHtml = newHtml
    
    return operation
  }

  /**
   * Get current state for debugging
   */
  getState() {
    return {
      html: this.htmlContent,
      version: this.version,
      pendingCount: this.pending.length,
      clientId: this.clientId
    }
  }
}