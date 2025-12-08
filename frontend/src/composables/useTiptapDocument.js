import { ref, computed } from 'vue'
import { HtmlDocumentState } from '@/utils/htmlOperationalTransform'

export function useTiptapDocument(clientId) {
  const documentState = new HtmlDocumentState(clientId)
  const htmlContent = ref('<p></p>')
  const isLoading = ref(false)
  
  const text = computed(() => documentState.textContent)
  const version = computed(() => documentState.version)
  const hasPendingOperations = computed(() => documentState.pending.length > 0)
  
  const handleSnapshot = (snapshot) => {
    const success = documentState.handleSnapshot(snapshot)
    if (success) {
      htmlContent.value = documentState.htmlContent
      isLoading.value = false
    }
    return success
  }
  
  const handleServerOperation = (op) => {
    const success = documentState.handleServerOperation(op)
    if (success) htmlContent.value = documentState.htmlContent
    return success
  }
  
  const handleHTMLInput = (newHTML) => {
    htmlContent.value = newHTML
    return documentState.handleHtmlChange(newHTML)
  }
  
  const reset = () => {
    documentState.updateFromHtml('<p></p>')
    htmlContent.value = '<p></p>'
    isLoading.value = false
  }
  
  const cleanup = () => {}
  
  return {
    htmlContent,
    isLoading,
    text,
    version,
    hasPendingOperations,
    handleSnapshot,
    handleServerOperation,
    handleHTMLInput,
    reset,
    cleanup,
    getDocumentState: () => ({
      html: htmlContent.value,
      text: documentState.textContent,
      version: documentState.version,
      pendingOps: documentState.pending.length
    })
  }
}