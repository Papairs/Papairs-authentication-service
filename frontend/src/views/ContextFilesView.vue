<template>
  <div class="min-h-screen bg-surface-light">
    <LoginHeader />
    
    <div class="max-w-4xl mx-auto px-8 py-12">
      <div class="flex items-center justify-between mb-8">
        <h1 class="text-4xl font-bold text-content-primary">
          Context Files
        </h1>
        
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
      
      <div class="bg-white rounded-lg shadow p-6">
        <h3 class="text-lg font-semibold text-content-primary mb-4">
          Manage Your Files
        </h3>
        
        <!-- Upload Section -->
        <div class="mb-6">
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
        <div class="space-y-2">
          <h4 class="text-sm font-medium text-content-primary mb-2">
            Your Files ({{ userFiles.length }})
          </h4>
          <div v-if="userFiles.length === 0" class="text-sm text-content-secondary italic p-4 text-center border border-border-light rounded">
            No files uploaded yet
          </div>
          <div
            v-for="file in userFiles"
            :key="file.fileId"
            class="flex items-center justify-between p-3 border border-border-light rounded hover:bg-surface-light transition-colors"
          >
            <div class="flex-1 min-w-0">
              <p class="text-sm font-medium text-content-primary truncate">
                {{ file.filename }}
              </p>
              <p class="text-xs text-content-secondary">{{ (file.fileSize / 1024).toFixed(1) }} KB</p>
            </div>
            <button
              @click="deleteFile(file.fileId)"
              class="ml-4 px-3 py-1 text-sm text-white bg-red-600 hover:bg-red-700 rounded transition-colors"
              title="Delete file"
            >
              Delete
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted } from 'vue'
import LoginHeader from '../components/LoginHeader.vue'
import { API_BASE_URL } from '@/config'
import axios from 'axios'
import auth from '@/utils/auth'

export default {
  name: 'ContextFilesView',
  components: {
    LoginHeader
  },
  setup() {
    // File management state
    const userFiles = ref([])
    const uploadError = ref(null)
    const uploadSuccess = ref(null)
    const isUploading = ref(false)
    const currentUploadFile = ref(null)
    const uploadAbortController = ref(null)
    const fileInputRef = ref(null)

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
        
        // Clear success message after 3 seconds
        setTimeout(() => {
          uploadSuccess.value = null
        }, 3000)
      } catch (error) {
        uploadError.value = error.response?.data?.error || error.response?.data?.message || 'Error deleting file'
      }
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
      userFiles,
      uploadError,
      uploadSuccess,
      isUploading,
      currentUploadFile,
      fileInputRef,
      uploadFile,
      deleteFile,
      isLoggedIn,
      userEmail,
      cancelOrDeleteUpload
    }
  }
}
</script>
