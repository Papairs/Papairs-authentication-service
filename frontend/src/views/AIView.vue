<template>
  <div class="min-h-screen bg-surface-light">
    <LoginHeader />
    
    <div class="max-w-7xl mx-auto px-8 py-12">
      <div class="flex items-center justify-between mb-8">
        <h1 class="text-4xl font-bold text-content-primary">
          AI Autocomplete with Context
        </h1>
        
        <!-- Mode Toggle -->
        <div class="flex items-center gap-4">
          <div class="flex items-center bg-white rounded-lg shadow px-2 py-1 border border-border-light">
            <button
              @click="aiMode = 'fast'"
              :class="[
                'px-3 py-1.5 rounded text-sm font-medium transition-all',
                aiMode === 'fast' 
                  ? 'bg-accent text-white' 
                  : 'text-content-secondary hover:text-content-primary'
              ]"
            >
              ⚡ Fast
            </button>
            <button
              @click="aiMode = 'advanced'"
              :class="[
                'px-3 py-1.5 rounded text-sm font-medium transition-all',
                aiMode === 'advanced' 
                  ? 'bg-accent text-white' 
                  : 'text-content-secondary hover:text-content-primary'
              ]"
            >
              🎯 Advanced
            </button>
          </div>
          
          <div v-if="!isLoggedIn" class="bg-yellow-50 border border-yellow-200 rounded-lg px-4 py-2">
            <router-link to="/login" class="text-yellow-800 hover:underline">
              Please log in to upload files
            </router-link>
          </div>
          <div v-else class="bg-green-50 border border-green-200 rounded-lg px-4 py-2">
            <p class="text-sm text-green-800">
              Logged in as: <strong>{{ userEmail }}</strong>
            </p>
          </div>
        </div>
      </div>
      
      <!-- Mode Info Banner -->
      <div v-if="aiMode === 'fast'" class="mb-6 p-4 bg-blue-50 border border-blue-200 rounded-lg">
        <p class="text-sm text-blue-800">
          <strong>⚡ Fast Mode:</strong> Quick responses with GPT-4o-mini. Supports text, markdown, and PDF files (local parsing).
        </p>
      </div>
      <div v-else class="mb-6 p-4 bg-purple-50 border border-purple-200 rounded-lg">
        <p class="text-sm text-purple-800">
          <strong>🎯 Advanced Mode:</strong> Slower but more accurate with GPT-4o + semantic search. Supports all file types (OpenAI handles parsing).
        </p>
      </div>
      
      <div class="grid grid-cols-1 lg:grid-cols-3 gap-8">
        <!-- File Management Panel (Left Side) -->
        <div class="lg:col-span-1">
          <div class="bg-white rounded-lg shadow p-6 sticky top-8">
            <h3 class="text-lg font-semibold text-content-primary mb-4">
              Context Files
            </h3>
            
            <!-- Upload Section -->
            <div class="mb-4">
              <label class="block mb-2 text-sm font-medium text-content-primary">
                Upload File
              </label>
              <div class="relative">
                <input
                  type="file"
                  @change="uploadFile"
                  accept=".txt,.md,.java,.js,.py,.html,.css,.pdf"
                  class="block w-full text-sm text-content-secondary file:mr-4 file:py-2 file:px-4 file:rounded file:border-0 file:text-sm file:font-semibold file:bg-accent file:text-white hover:file:bg-accent-hover cursor-pointer"
                  :disabled="isUploading"
                  ref="fileInputRef"
                />
                <button
                  v-if="isUploading || currentUploadFile"
                  @click="cancelOrDeleteUpload"
                  class="absolute right-2 top-1/2 -translate-y-1/2 text-red-600 hover:text-red-800 text-2xl font-bold bg-white rounded-full w-8 h-8 flex items-center justify-center shadow-md hover:shadow-lg transition-all"
                  :title="isUploading ? 'Cancel upload' : 'Delete uploaded file'"
                >
                  ×
                </button>
              </div>
              <p v-if="uploadError" class="mt-2 text-sm text-red-600">{{ uploadError }}</p>
              <p v-if="uploadSuccess" class="mt-2 text-sm text-green-600">{{ uploadSuccess }}</p>
              <p v-if="isUploading" class="mt-2 text-sm text-content-secondary">Uploading {{ currentUploadFile }}...</p>
            </div>

            <!-- Files List -->
            <div class="space-y-2 max-h-96 overflow-y-auto">
              <h4 class="text-sm font-medium text-content-primary mb-2">
                Your Files ({{ userFiles.length }})
              </h4>
              <div v-if="userFiles.length === 0" class="text-sm text-content-secondary italic">
                No files uploaded yet
              </div>
              <div
                v-for="file in userFiles"
                :key="file.fileId"
                class="flex items-center justify-between p-2 border border-border-light rounded hover:bg-surface-light transition-colors"
              >
                <label class="flex items-center flex-1 cursor-pointer">
                  <input
                    type="checkbox"
                    :checked="isFileSelected(file.fileId)"
                    @change="toggleFileSelection(file)"
                    class="mr-2 w-4 h-4 text-accent bg-gray-100 border-gray-300 rounded focus:ring-accent"
                  />
                  <div class="flex-1 min-w-0">
                    <p class="text-sm font-medium text-content-primary truncate">
                      {{ file.filename }}
                    </p>
                    <p class="text-xs text-content-secondary">{{ (file.fileSize / 1024).toFixed(1) }} KB</p>
                  </div>
                </label>
                <button
                  @click="deleteFile(file.fileId)"
                  class="ml-2 text-red-600 hover:text-red-800 text-lg font-bold"
                  title="Delete file"
                >
                  ×
                </button>
              </div>
            </div>

            <div v-if="selectedFiles.length > 0" class="mt-4 p-3 bg-blue-50 rounded text-sm">
              <p class="font-medium text-blue-900">
                {{ selectedFiles.length }} file(s) selected as context
              </p>
            </div>
          </div>
        </div>

        <!-- Autocomplete Editor (Right Side) -->
        <div class="lg:col-span-2">
          <div class="bg-white rounded-lg shadow p-6">
            <h3 class="text-lg font-semibold text-content-primary mb-4">
              AI Autocomplete
            </h3>
            
            <div class="relative mb-4">
              <div class="relative">
                <textarea
                  ref="textareaRef"
                  v-model="input"
                  @keydown="handleKeyDown"
                  @input="updateMirror"
                  @scroll="syncScroll"
                  class="w-full h-96 p-5 text-base font-mono bg-white border border-border-light rounded-lg resize-y outline-none transition-shadow focus:border-accent focus:shadow-md relative z-0 text-content-primary"
                  :placeholder="isFocused ? '' : 'Start typing...'"
                  @focus="isFocused = true"
                  @blur="isFocused = false"
                ></textarea>
                
                <div 
                  v-if="suggestion && input"
                  ref="mirrorRef"
                  class="absolute top-0 left-0 w-full h-96 p-5 text-base font-mono pointer-events-none whitespace-pre-wrap break-words overflow-hidden rounded-lg z-10"
                  :style="{ lineHeight: '1.5' }"
                >
                  <span class="invisible">{{ input }}</span><span class="text-accent opacity-50 italic">{{ displaySuggestion }}</span>
                </div>
              </div>
            </div>
            
            <div class="flex items-center justify-between">
              <p class="text-sm text-content-secondary">
                Press <strong class="text-content-primary bg-border-light px-2 py-1 rounded">Tab</strong> to accept suggestion
              </p>
              <p v-if="isLoading" class="text-sm text-accent">
                Generating...
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, watch, computed, onMounted } from 'vue'
import LoginHeader from '../components/LoginHeader.vue'
import { API_BASE_URL } from '@/config'
import axios from 'axios'
import auth from '@/utils/auth'

export default {
  name: 'AIView',
  components: {
    LoginHeader
  },
  setup() {
    // AI Mode state
    const aiMode = ref('fast') // 'fast' or 'advanced'
    
    // Autocomplete state
    const input = ref('')
    const suggestion = ref('')
    const isFocused = ref(false)
    const isLoading = ref(false)
    const textareaRef = ref(null)
    const mirrorRef = ref(null)
    let timer = null

    // File management state
    const userFiles = ref([])
    const selectedFiles = ref([])
    const uploadError = ref(null)
    const uploadSuccess = ref(null)
    const isUploading = ref(false)
    const currentUploadFile = ref(null)
    const uploadAbortController = ref(null)

    const displaySuggestion = computed(() => {
      if (!suggestion.value) return ''
      
      if (suggestion.value.startsWith(' ')) return suggestion.value
      if (input.value.endsWith(' ')) return suggestion.value
      
      return ' ' + suggestion.value
    })

    watch(input, (newValue) => {
      suggestion.value = ''
      if (timer) clearTimeout(timer)
      
      if (newValue.trim()) {
        timer = setTimeout(() => fetchSuggestion(newValue), 1000)
      }
    })

    const fetchSuggestion = async (text) => {
      if (!text.trim()) {
        suggestion.value = ''
        return
      }

      isLoading.value = true
      try {
        const requestBody = { 
          userInput: text,
          mode: aiMode.value,
          selectedFiles: selectedFiles.value.map(f => ({
            fileId: f.fileId,
            filename: f.filename,
            mimeType: f.mimeType
          }))
        }
        
        const authHeaders = await auth.getAuthHeaders()
        const response = await fetch(`${API_BASE_URL}/api/ai/autocomplete`, {
          method: 'POST',
          headers: { 
            'Content-Type': 'application/json',
            ...authHeaders
          },
          body: JSON.stringify(requestBody)
        })
        
        if (!response.ok) {
          const errorText = await response.text()
          console.error('API error:', response.status, errorText)
          suggestion.value = ''
          return
        }
        
        const data = await response.json()
        suggestion.value = data.suggestion || ''
      } catch (error) {
        console.error('Error fetching suggestion:', error)
        suggestion.value = ''
      } finally {
        isLoading.value = false
      }
    }

    const handleKeyDown = (event) => {
      if (event.key === 'Tab' && suggestion.value) {
        event.preventDefault()
        input.value += displaySuggestion.value
        suggestion.value = ''
      }
    }

    const updateMirror = () => {
      if (mirrorRef.value && textareaRef.value) {
        mirrorRef.value.scrollTop = textareaRef.value.scrollTop
        mirrorRef.value.scrollLeft = textareaRef.value.scrollLeft
      }
    }

    const syncScroll = () => {
      if (mirrorRef.value && textareaRef.value) {
        mirrorRef.value.scrollTop = textareaRef.value.scrollTop
        mirrorRef.value.scrollLeft = textareaRef.value.scrollLeft
      }
    }

    // File management methods
    const loadUserFiles = async () => {
      const token = auth.getToken()
      if (!token) return

      try {
        const response = await axios.get(
            `${API_BASE_URL}/api/docs/files`,
            {
              headers: {
                'Authorization': `Bearer ${token}`
              }
            }
        )
        userFiles.value = response.data
      } catch (error) {
        console.error('Error loading files:', error)
      }
    }

    const uploadFile = async (event) => {
      const file = event.target.files[0]
      if (!file) return

      uploadError.value = null
      uploadSuccess.value = null
      isUploading.value = true
      currentUploadFile.value = file.name
      uploadAbortController.value = new AbortController()

      const token = auth.getToken()
      if (!token) {
        uploadError.value = 'Token not found. Please log in.'
        isUploading.value = false
        currentUploadFile.value = null
        uploadAbortController.value = null
        return
      }

      const formData = new FormData()
      formData.append('file', file)

      try {
        await axios.post(`${API_BASE_URL}/api/docs/files`, formData, {
          headers: {
            'Authorization': `Bearer ${token}`
          },
          signal: uploadAbortController.value.signal
        })

        uploadSuccess.value = `File "${file.name}" uploaded successfully!`
        await loadUserFiles()
        event.target.value = ''

        setTimeout(() => {
          uploadSuccess.value = null
        }, 3000)
      } catch (error) {
        if (error.name === 'CanceledError' || error.code === 'ERR_CANCELED') {
          uploadError.value = 'Upload canceled'
        } else if (error.response?.status === 409) {
          uploadError.value = `File "${file.name}" already exists`
        } else {
          uploadError.value = error.response?.data?.message || 'Error uploading file'
        }
      } finally {
        isUploading.value = false
        currentUploadFile.value = null
        uploadAbortController.value = null
      }
    }

    const cancelOrDeleteUpload = async () => {
      if (isUploading.value && uploadAbortController.value) {
        uploadAbortController.value.abort()
        uploadError.value = 'Upload canceled'
        isUploading.value = false
        currentUploadFile.value = null
        uploadAbortController.value = null
      } else if (currentUploadFile.value) {
        const fileToDelete = userFiles.value.find(f => f.filename === currentUploadFile.value)
        if (fileToDelete) {
          await deleteFile(fileToDelete.fileId)
          currentUploadFile.value = null
        }
      }
    }

    const deleteFile = async (fileId) => {
      try {
        const token = auth.getToken()
        if (!token) {
          uploadError.value = 'Token not found. Please log in.'
          return
        }

        await axios.delete(
            `${API_BASE_URL}/api/docs/files/${fileId}`,
            {
              headers: {
                'Authorization': `Bearer ${token}`
              }
            }
        )
        uploadSuccess.value = 'File deleted successfully'
        await loadUserFiles()
        selectedFiles.value = selectedFiles.value.filter(f => f.fileId !== fileId)
        
        // Clear success message after 3 seconds
        setTimeout(() => {
          uploadSuccess.value = null
        }, 3000)
      } catch (error) {
        uploadError.value = error.response?.data?.error || error.response?.data?.message || 'Error deleting file'
      }
    }

    const toggleFileSelection = (file) => {
      const index = selectedFiles.value.findIndex(f => f.fileId === file.fileId)
      if (index === -1) {
        selectedFiles.value.push(file)
      } else {
        selectedFiles.value.splice(index, 1)
      }
    }

    const isFileSelected = (fileId) => {
      return selectedFiles.value.some(f => f.fileId === fileId)
    }

    const isLoggedIn = computed(() => {
      return auth.isAuthenticated()
    })

    const userEmail = computed(() => {
      return auth.getUserEmail() || 'Unknown'
    })

    onMounted(() => {
      loadUserFiles()
    })

    return {
      aiMode,
      input,
      suggestion,
      isFocused,
      isLoading,
      textareaRef,
      mirrorRef,
      displaySuggestion,
      handleKeyDown,
      updateMirror,
      syncScroll,
      userFiles,
      selectedFiles,
      uploadError,
      uploadSuccess,
      isUploading,
      currentUploadFile,
      uploadFile,
      deleteFile,
      toggleFileSelection,
      isFileSelected,
      isLoggedIn,
      userEmail,
      cancelOrDeleteUpload
    }
  }
}
</script>
