<template>
  <transition name="slide-left">
    <div v-if="show" class="absolute left-0 top-0 h-full w-80 bg-surface-light border-r-2 border-border-opa overflow-y-auto">
      <div class="sticky top-0 bg-surface-light border-b border-border-opa p-4 flex justify-between items-center">
        <h3 class="font-semibold text-content-primary">Context Files</h3>
        <button @click="$emit('close')" class="text-content-secondary hover:text-content-primary transition-colors">✕</button>
      </div>
      
      <!-- Upload Section -->
      <div class="p-4">
        <label class="block mb-2 text-sm font-medium text-content-primary">
          Upload File
        </label>
        <div class="relative">
          <input
            type="file"
            @change="handleFileChange"
            accept=".txt,.md,.java,.js,.py,.html,.css,.pdf"
            class="block w-full text-xs text-content-secondary file:mr-2 file:py-1.5 file:px-3 file:rounded file:border-0 file:text-xs file:font-semibold file:bg-accent file:text-white hover:file:bg-accent-hover cursor-pointer"
            :disabled="isUploading"
            ref="fileInput"
          />
        </div>
        <p v-if="uploadError" class="mt-2 text-xs text-red-600">{{ uploadError }}</p>
        <p v-if="uploadSuccess" class="mt-2 text-xs text-green-600">{{ uploadSuccess }}</p>
        <p v-if="isUploading" class="mt-2 text-xs text-content-secondary">Uploading {{ currentUploadFile }}...</p>
      </div>

      <!-- Files List -->
      <div class="px-4 pb-4">
        <h4 class="text-sm font-medium text-content-primary mb-2">
          Your Files ({{ files.length }})
        </h4>
        <div v-if="files.length === 0" class="text-xs text-content-secondary italic">
          No files uploaded yet
        </div>
        <div class="space-y-2">
          <div
            v-for="file in files"
            :key="file.fileId"
            class="flex items-center justify-between p-2 border border-border-light rounded hover:bg-surface-light-secondary transition-colors"
          >
            <label class="flex items-center flex-1 cursor-pointer min-w-0">
              <input
                type="checkbox"
                :checked="isFileSelected(file.fileId)"
                @change="$emit('toggle-selection', file)"
                class="mr-2 w-3 h-3 text-accent bg-gray-100 border-gray-300 rounded focus:ring-accent flex-shrink-0"
              />
              <div class="flex-1 min-w-0">
                <p class="text-xs font-medium text-content-primary truncate">
                  {{ file.filename }}
                </p>
                <p class="text-xs text-content-secondary">{{ (file.fileSize / 1024).toFixed(1) }} KB</p>
              </div>
            </label>
            <button
              @click="$emit('delete', file.fileId)"
              class="ml-2 text-red-600 hover:text-red-800 text-lg font-bold flex-shrink-0"
              title="Delete file"
            >
              ×
            </button>
          </div>
        </div>
        
        <div v-if="selectedFiles.length > 0" class="mt-3 p-2 bg-blue-50 rounded text-xs">
          <p class="font-medium text-blue-900">
            {{ selectedFiles.length }} file(s) selected as context
          </p>
        </div>
      </div>
    </div>
  </transition>
</template>

<script>
export default {
  name: 'ContextFilesPanel',
  props: {
    show: {
      type: Boolean,
      required: true
    },
    files: {
      type: Array,
      default: () => []
    },
    selectedFiles: {
      type: Array,
      default: () => []
    },
    uploadError: {
      type: String,
      default: null
    },
    uploadSuccess: {
      type: String,
      default: null
    },
    isUploading: {
      type: Boolean,
      default: false
    },
    currentUploadFile: {
      type: String,
      default: null
    }
  },
  emits: ['close', 'upload', 'delete', 'toggle-selection'],
  methods: {
    isFileSelected(fileId) {
      return this.selectedFiles.some(f => f.fileId === fileId)
    },
    handleFileChange(event) {
      this.$emit('upload', event)
    }
  }
}
</script>

<style scoped>
.slide-left-enter-active, .slide-left-leave-active {
  transition: transform 0.3s ease-out;
}
.slide-left-enter-from {
  transform: translateX(-100%);
}
.slide-left-leave-to {
  transform: translateX(-100%);
}
</style>
