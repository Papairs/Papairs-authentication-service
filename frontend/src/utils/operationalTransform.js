/**
 * Clever Operational Transform - Quality over Quantity
 */

// Smart utility - handles bounds automatically
const clamp = (val, min, max) => Math.max(min, Math.min(max, val))

// Operation creators with auto-generated IDs
export const op = {
  insert: (pos, text, version, clientId) => ({ 
    type: 'insert', pos, text, baseVersion: version, clientId, opId: crypto.randomUUID() 
  }),
  delete: (pos, length, version, clientId) => ({ 
    type: 'delete', pos, length, baseVersion: version, clientId, opId: crypto.randomUUID() 
  })
}

// Apply single operation to text
export const apply = (text, { type, pos, text: insertText, length }) => {
  if (type === 'insert') {
    const safePos = clamp(pos, 0, text.length)
    return text.slice(0, safePos) + insertText + text.slice(safePos)
  }
  const safePos = clamp(pos, 0, text.length)
  const safeEnd = clamp(safePos + length, 0, text.length)
  return text.slice(0, safePos) + text.slice(safeEnd)
}

// Apply multiple operations
export const applyAll = (text, operations) => operations.reduce(apply, text)

// Calculate text difference
export const diff = (oldText, newText) => {
  if (oldText === newText) return null
  
  let start = 0
  const minLen = Math.min(oldText.length, newText.length)
  
  // Find start of difference
  while (start < minLen && oldText[start] === newText[start]) start++
  
  // Find end of difference
  let endOld = oldText.length - 1
  let endNew = newText.length - 1
  while (endOld >= start && endNew >= start && oldText[endOld] === newText[endNew]) {
    endOld--
    endNew--
  }
  
  return oldText.length < newText.length
    ? { type: 'insert', pos: start, text: newText.slice(start, endNew + 1) }
    : { type: 'delete', pos: start, length: endOld - start + 1 }
}

// Transform operation A against B - clever logic
export const transform = (opA, opB) => {
  const result = { ...opA }
  const [a, b] = [opA, opB]
  
  // Tiebreaker for same position
  const tiebreak = () => `${a.clientId}:${a.opId}`.localeCompare(`${b.clientId}:${b.opId}`) > 0
  
  if (a.type === 'insert' && b.type === 'insert') {
    if (a.pos > b.pos || (a.pos === b.pos && tiebreak())) {
      result.pos += b.text.length
    }
  } else if (a.type === 'insert' && b.type === 'delete') {
    if (a.pos > b.pos) result.pos -= Math.min(b.length, a.pos - b.pos)
  } else if (a.type === 'delete' && b.type === 'insert') {
    if (a.pos >= b.pos) result.pos += b.text.length
  } else if (a.type === 'delete' && b.type === 'delete') {
    if (b.pos + b.length <= a.pos) {
      result.pos -= b.length
    } else if (b.pos < a.pos + a.length) {
      const overlap = Math.max(0, Math.min(a.pos + a.length, b.pos + b.length) - Math.max(a.pos, b.pos))
      result.length -= overlap
      if (b.pos < a.pos) result.pos = b.pos
    }
  }
  
  return result
}

// Create operation from text change
export const createOperation = (type, pos, data, version, clientId) => 
  op[type](pos, data, version, clientId)

// Validate operation
export const isValid = (operation) => 
  operation?.type && ['insert', 'delete'].includes(operation.type) && 
  typeof operation.pos === 'number' && operation.pos >= 0