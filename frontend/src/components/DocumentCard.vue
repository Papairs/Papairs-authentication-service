<template>
  <div 
    class="group relative bg-white dark:bg-surface-dark-secondary border border-border-light dark:border-border-dark rounded-lg p-4 hover:shadow-lg hover:border-accent transition-all cursor-pointer"
    @click="$emit('click')"
  >
    <div class="flex flex-col items-center">
      <!-- Document Icon -->
      <div class="text-5xl mb-2">📄</div>
      
      <!-- Document Title -->
      <h3 class="text-sm font-medium text-content-primary dark:text-content-inverse text-center truncate w-full">
        {{ document.title }}
      </h3>
      
      <!-- Updated Date -->
      <p class="text-xs text-content-secondary mt-1">
        {{ formatDate(document.updatedAt) }}
      </p>
    </div>

    <!-- Actions Menu (appears on hover) -->
    <div class="absolute top-2 right-2 opacity-0 group-hover:opacity-100 transition-opacity">
      <button 
        @click.stop="showMenu = !showMenu"
        class="p-1 rounded-full hover:bg-surface-light-secondary dark:hover:bg-surface-dark"
      >
        <svg class="w-5 h-5 text-content-secondary" fill="currentColor" viewBox="0 0 20 20">
          <path d="M10 6a2 2 0 110-4 2 2 0 010 4zM10 12a2 2 0 110-4 2 2 0 010 4zM10 18a2 2 0 110-4 2 2 0 010 4z" />
        </svg>
      </button>
      
      <!-- Dropdown Menu -->
      <div 
        v-if="showMenu"
        class="absolute right-0 mt-2 w-40 bg-white dark:bg-surface-dark-secondary border border-border-light dark:border-border-dark rounded-lg shadow-lg z-10"
      >
        <button 
          @click.stop="handleRename"
          class="w-full text-left px-4 py-2 text-sm text-content-primary dark:text-content-inverse hover:bg-surface-light-secondary dark:hover:bg-surface-dark"
        >
          Rename
        </button>
        <button 
          @click.stop="handleDelete"
          class="w-full text-left px-4 py-2 text-sm text-red-600 hover:bg-red-50 dark:hover:bg-red-900"
        >
          Delete
        </button>
      </div>
    </div>
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
