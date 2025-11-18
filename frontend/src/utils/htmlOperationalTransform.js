/**
 * HTML-aware Operational Transform utilities for Tiptap integration
 * Provides better handling of HTML content changes while maintaining OT compatibility
 */

import { diff, apply, applyAll, transform, createOperation, isValid, op } from './operationalTransform'

/**
 * Convert HTML to plain text for OT operations
 * Preserves line breaks and basic structure
 */
export const htmlToText = (html) => {
  if (!html) return ''
  
  // Create a temporary DOM element to strip HTML tags
  const tempDiv = document.createElement('div')
  tempDiv.innerHTML = html
  
  // Convert block elements to newlines to preserve structure
  const blockElements = tempDiv.querySelectorAll('p, div, h1, h2, h3, h4, h5, h6, li, br')
  blockElements.forEach(el => {
    if (el.tagName === 'BR') {
      el.replaceWith('\n')
    } else if (el.tagName !== 'LI') {
      el.appendChild(document.createTextNode('\n'))
    }
  })
  
  return (tempDiv.textContent || tempDiv.innerText || '').replace(/\n+/g, '\n').trim()
}

/**
 * Calculate difference between two HTML strings by comparing their text content
 * This allows us to use existing OT logic while working with HTML
 */
export const htmlDiff = (oldHtml, newHtml) => {
  const oldText = htmlToText(oldHtml)
  const newText = htmlToText(newHtml)
  return diff(oldText, newText)
}

/**
 * Apply text-based operation to HTML content
 * Attempts to preserve HTML structure where possible
 */
export const applyToHtml = (html, operation) => {
  try {
    const text = htmlToText(html)
    const newText = apply(text, operation)
    
    // If the content is simple (no complex formatting), wrap in paragraph
    if (!newText || newText.trim() === '') {
      return '<p></p>'
    }
    
    // Try to preserve basic HTML structure
    if (html.includes('<h1>') || html.includes('<h2>') || html.includes('<h3>')) {
      // Preserve heading structure if possible
      const lines = newText.split('\n').filter(line => line.trim())
      if (lines.length === 1) {
        // Single line - try to guess heading level
        const headingMatch = html.match(/<(h[1-6])>/)
        if (headingMatch) {
          return `<${headingMatch[1]}>${escapeHtml(lines[0])}</${headingMatch[1]}>`
        }
      }
    }
    
    // Check for list content
    if (html.includes('<li>')) {
      const lines = newText.split('\n').filter(line => line.trim())
      if (lines.length > 1) {
        const listItems = lines.map(line => `<li>${escapeHtml(line)}</li>`).join('')
        return html.includes('<ol>') ? `<ol>${listItems}</ol>` : `<ul>${listItems}</ul>`
      }
    }
    
    // Default: wrap in paragraphs
    const lines = newText.split('\n').filter(line => line.trim())
    if (lines.length === 1) {
      return `<p>${escapeHtml(lines[0])}</p>`
    } else if (lines.length > 1) {
      return lines.map(line => `<p>${escapeHtml(line)}</p>`).join('')
    }
    
    return '<p></p>'
  } catch (error) {
    console.error('Error applying operation to HTML:', error)
    return html // Return original on error
  }
}

/**
 * Escape HTML characters
 */
export const escapeHtml = (text) => {
  const div = document.createElement('div')
  div.textContent = text
  return div.innerHTML
}

/**
 * Create HTML-aware operation from content changes
 */
export const createHtmlOperation = (oldHtml, newHtml, version, clientId) => {
  const change = htmlDiff(oldHtml, newHtml)
  if (!change) return null
  
  return createOperation(
    change.type,
    change.pos,
    change.text || change.length,
    version,
    clientId
  )
}

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
      console.warn('[HtmlDocumentState] Invalid operation or missing HTML content')
      return false
    }

    try {
      // Check if it's our own operation
      const isOurs = this.pending[0]?.opId === op.opId && 
                     this.pending[0]?.clientId === op.clientId

      if (isOurs) {
        this.pending.shift()
        // For our own operations, we already have the correct HTML
        this.version = op.version || this.version + 1
        return true
      }

      // For other users' operations, apply the HTML content directly
      this.htmlContent = op.htmlContent
      this.version = op.version || this.version + 1
      this.lastHtml = this.htmlContent
      
      console.log('[HtmlDocumentState] Applied server HTML operation, new version:', this.version)
      return true
    } catch (error) {
      console.error('[HtmlDocumentState] Error handling server operation:', error)
      return false
    }
  }

  /**
   * Handle local HTML changes - now creates HTML-based operations
   */
  handleHtmlChange(newHtml) {
    newHtml = newHtml || '<p></p>'
    
    // Check if HTML actually changed
    if (this.lastHtml === newHtml) {
      return null
    }
    
    // Always update HTML content to preserve formatting
    this.htmlContent = newHtml

    // Create HTML operation with the full content
    const operation = {
      type: 'html-update',
      htmlContent: newHtml,
      version: this.version,
      clientId: this.clientId,
      opId: crypto.randomUUID(),
      baseVersion: this.version + this.pending.length,
      timestamp: Date.now()
    }

    // Add to pending operations
    this.pending.push(operation)
    
    // Update last HTML
    this.lastHtml = newHtml
    
    console.log('[HtmlDocumentState] Created HTML operation:', {
      opId: operation.opId,
      version: operation.version,
      htmlLength: newHtml.length
    })
    
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

// Re-export original utilities for backwards compatibility
export { 
  diff, 
  apply, 
  applyAll, 
  transform, 
  createOperation, 
  isValid, 
  op 
}