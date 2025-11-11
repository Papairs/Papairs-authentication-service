/**
 * Simple, clever error handling utilities
 * Quality over quantity approach
 */

// Core error logging with smart categorization
const logError = (context, message, error = null) => {
  const prefix = `[${context}]`
  if (error) {
    console.error(`${prefix} ${message}:`, error)
  } else {
    console.warn(`${prefix} ${message}`)
  }
}

// Safe execution wrapper - handles both sync and async
export const safe = (fn, context = 'Operation', fallback = null) => {
  try {
    const result = fn()
    return result instanceof Promise 
      ? result.catch(err => {
          logError(context, 'Async operation failed', err)
          return fallback
        })
      : result
  } catch (error) {
    logError(context, 'Operation failed', error)
    return fallback
  }
}

// Retry with exponential backoff
export const retry = async (fn, maxAttempts = 3, delay = 500, context = 'Retry') => {
  for (let attempt = 1; attempt <= maxAttempts; attempt++) {
    try {
      return await fn()
    } catch (error) {
      if (attempt === maxAttempts) throw error
      const waitTime = delay * Math.pow(2, attempt - 1)
      logError(context, `Attempt ${attempt}/${maxAttempts} failed, retrying in ${waitTime}ms`)
      await new Promise(resolve => setTimeout(resolve, waitTime))
    }
  }
}

// Specific error handlers - simple and focused
export const handleWebSocketError = (message, error) => logError('WebSocket', message, error)
export const handleDocumentError = (message, error) => logError('Document', message, error)

// Create a simple error handler for component use
export const createErrorHandler = () => ({
  websocket: (msg, err) => handleWebSocketError(msg, err),
  document: (msg, err) => handleDocumentError(msg, err),
  safe: (fn, fallback) => safe(fn, 'Handler', fallback),
  retry: (fn, attempts) => retry(fn, attempts, 500, 'Handler')
})