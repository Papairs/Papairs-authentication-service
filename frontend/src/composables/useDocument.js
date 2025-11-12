import { ref, computed } from 'vue'
import { apply, applyAll, transform, diff, createOperation, isValid } from '@/utils/operationalTransform'

export function useDocument(clientId) {
  // Core state
  const text = ref('')
  const mirror = ref('') // Server state
  const version = ref(0)
  const pending = ref([])
  const lastValue = ref('')

  // Smart updater - handles all text state logic
  const updateText = () => {
    text.value = applyAll(mirror.value, pending.value)
    lastValue.value = text.value
  }

  // Handle server snapshot
  const handleSnapshot = (snapshot) => {
    mirror.value = snapshot.content || ''
    version.value = snapshot.version
    updateText()
    console.log(`[Document] Snapshot v${version.value}`)
    return true
  }

  // Handle server operation - clever logic
  const handleServerOperation = (op) => {
    if (!isValid(op)) return false
    
    // Apply to server state
    mirror.value = apply(mirror.value, op)
    version.value = op.version

    // Check if it's our own operation
    const isOurs = pending.value[0]?.opId === op.opId && pending.value[0]?.clientId === op.clientId
    
    if (isOurs) {
      console.log('[Document] Ack:', op.opId)
      pending.value.shift()
    } else {
      console.log('[Document] Transform pending ops')
      pending.value = pending.value.map(p => transform(p, op))
    }

    updateText()
    console.log(`[Document] v${version.value}, pending: ${pending.value.length}`)
    return true
  }

  // Handle text input - creates and queues operations
  const handleTextInput = (newValue) => {
    const change = diff(lastValue.value, newValue)
    if (!change) return null

    const operation = createOperation(change.type, change.pos, change.text || change.length, version.value, clientId)
    console.log('[Document] Local edit:', operation.type, 'at', operation.pos)
    
    pending.value.push(operation)
    updateText()
    return operation
  }

  // Reset everything
  const reset = () => {
    text.value = mirror.value = lastValue.value = ''
    version.value = 0
    pending.value = []
  }

  return {
    // State
    text,
    version,
    hasPendingOperations: computed(() => pending.value.length > 0),
    
    // Methods
    handleSnapshot,
    handleServerOperation,
    handleTextInput,
    resetDocument: reset,
    
    // Debug helpers
    getDocumentState: () => ({ text: text.value, mirror: mirror.value, version: version.value, pending: pending.value.length }),
    mirror: computed(() => mirror.value),
    pendingOperations: computed(() => pending.value)
  }
}