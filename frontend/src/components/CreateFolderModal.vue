<template>
  <div class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50" @click.self="$emit('close')">
    <div class="bg-white rounded-lg shadow-xl p-6 w-full max-w-md">
      <h2 class="text-xl font-semibold text-content-primary mb-4">
        Create New Folder
      </h2>
      
      <form @submit.prevent="handleSubmit">
        <div class="mb-4">
          <label class="block text-sm font-medium text-content-primary mb-2">
            Folder Name
          </label>
          <input 
            v-model="folderName"
            type="text"
            placeholder="Enter folder name..."
            class="w-full px-4 py-2 border border-border-light rounded-lg focus:outline-none focus:border-accent bg-white text-content-primary"
            :disabled="loading"
            autofocus
          />
        </div>

        <div v-if="error" class="mb-4 p-3 bg-red-100 border border-red-300 rounded-lg">
          <p class="text-sm text-red-700">{{ error }}</p>
        </div>

        <div class="flex justify-end space-x-3">
          <button 
            type="button"
            @click="$emit('close')"
            class="px-4 py-2 border border-border-light rounded-lg text-content-primary hover:bg-surface-light-secondary"
            :disabled="loading"
          >
            Cancel
          </button>
          <button 
            type="submit"
            class="px-4 py-2 bg-accent hover:bg-[#E66900] text-white rounded-lg disabled:bg-gray-400 disabled:cursor-not-allowed"
            :disabled="!folderName.trim() || loading"
          >
            {{ loading ? 'Creating...' : 'Create' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script>
import { ref } from 'vue'
import { driveService } from '@/utils/driveService'

export default {
  name: 'CreateFolderModal',
  props: {
    parentFolderId: {
      type: String,
      default: null
    }
  },
  emits: ['close', 'created'],
  setup(props, { emit }) {
    const folderName = ref('')
    const loading = ref(false)
    const error = ref(null)

    const handleSubmit = async () => {
      error.value = null
      loading.value = true

      try {
        const folder = await driveService.createFolder(folderName.value.trim(), props.parentFolderId)
        emit('created', folder)
      } catch (err) {
        console.error('Error creating folder:', err)
        error.value = err.response?.data?.message || 'Failed to create folder. Please try again.'
      } finally {
        loading.value = false
      }
    }

    return {
      folderName,
      loading,
      error,
      handleSubmit
    }
  }
}
</script>
