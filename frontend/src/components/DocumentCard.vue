<template>
  <div 
    class="group relative bg-white dark:bg-surface-dark-secondary border border-border-light dark:border-border-dark rounded-xl hover:shadow-md transition-all cursor-pointer overflow-hidden"
    style="aspect-ratio: 1; width: 100%"
    @click="$emit('click')"
  >
    <!-- Header with Icon, Title and Menu on same line -->
    <div class="flex items-center justify-between p-4">
      <!-- Icon and Title -->
      <div class="flex items-center space-x-3 flex-1 min-w-0">
        <!-- Document Icon -->
        <div class="flex-shrink-0">
          <svg class="w-5 h-5 text-content-primary dark:text-content-inverse" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
          </svg>
        </div>
        
        <!-- Document Title -->
        <h3 class="text-sm font-medium text-content-primary dark:text-content-inverse truncate">
          {{ document.title }}
        </h3>
      </div>
      
      <!-- Menu Button -->
      <button 
        @click.stop="showMenu = !showMenu"
        class="flex-shrink-0 p-1 rounded hover:bg-surface-light-secondary dark:hover:bg-surface-dark transition-colors"
      >
        <svg class="w-5 h-5 text-gray-700 dark:text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16" />
        </svg>
      </button>
      
      <!-- Dropdown Menu -->
      <div 
        v-if="showMenu"
        class="absolute right-4 top-12 w-40 bg-white dark:bg-surface-dark-secondary border border-border-light dark:border-border-dark rounded-lg shadow-lg z-10"
      >
        <button 
          @click.stop="handleRename"
          class="w-full text-left px-4 py-2 text-sm text-content-primary dark:text-content-inverse hover:bg-surface-light-secondary dark:hover:bg-surface-dark first:rounded-t-lg"
        >
          Rename
        </button>
        <button 
          @click.stop="handleDelete"
          class="w-full text-left px-4 py-2 text-sm text-red-600 hover:bg-red-50 dark:hover:bg-red-900 last:rounded-b-lg"
        >
          Delete
        </button>
      </div>
    </div>

    <!-- Whitespace area for visual breathing room -->
    <div class="flex-1"></div>
  </div>
</template>

<script>
import { ref } from 'vue'

export default {
  name: 'DocumentCard',
  props: {
    document: {
      type: Object,
      required: true
    }
  },
  emits: ['click', 'delete', 'rename'],
  setup(props, { emit }) {
    const showMenu = ref(false)

    const formatDate = (dateString) => {
      if (!dateString) return ''
      const date = new Date(dateString)
      return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })
    }

    const handleDelete = () => {
      showMenu.value = false
      emit('delete')
    }

    const handleRename = () => {
      showMenu.value = false
      emit('rename')
    }

    return {
      showMenu,
      formatDate,
      handleDelete,
      handleRename
    }
  }
}
</script>
