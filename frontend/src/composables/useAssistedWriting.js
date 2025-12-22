import { ref, watch } from 'vue'

const STORAGE_KEY = 'papairs-assisted-writing'

// Shared state across all components
const isEnabled = ref(localStorage.getItem(STORAGE_KEY) === 'true')

// Watch for changes and persist to localStorage
watch(isEnabled, (newValue) => {
  localStorage.setItem(STORAGE_KEY, newValue.toString())
})

export function useAssistedWriting() {
  const toggleAssistedWriting = () => {
    isEnabled.value = !isEnabled.value
  }

  return {
    isAssistedWritingEnabled: isEnabled,
    toggleAssistedWriting
  }
}
