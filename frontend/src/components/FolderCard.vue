
<template>
  <div 
    class="group relative bg-surface-light border border-content-primary rounded-lg hover:shadow-md transition-all cursor-pointer overflow-visible"
    style="height: 50px; width: 100%"
    @click="$emit('click')"
  >
    <!-- Header with Icon, Title and Menu on same line -->
    <div class="flex items-center justify-between px-4 h-full">
      <!-- Icon and Title -->
      <div class="flex items-center space-x-3 flex-1 min-w-0">
        <!-- Folder Icon -->
        <div class="flex-shrink-0">
          <svg class="w-5 h-5 text-content-primary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z" />
          </svg>
        </div>
        
        <!-- Folder Name -->
        <h3 class="text-sm font-medium text-content-primary truncate">
          {{ folder.name }}
        </h3>
      </div>
      
      <!-- Menu Button Container -->
      <div class="relative" ref="menuRef">
        <button 
          @click.stop="showMenu = !showMenu"
          class="flex-shrink-0 p-1 rounded hover:bg-surface-light-secondary transition-colors"
        >
          <MenuIcon :size="20" class-name="text-content-primary" />
        </button>
        
        <!-- Dropdown Menu -->
        <div 
          v-if="showMenu"
          @click.stop
          class="absolute right-0 top-full mt-1 w-40 bg-surface-light border border-content-primary rounded-lg shadow-lg z-50 text-content-primary text-sm"
        >
          <button 
            @click.stop="handleRename"
            class="w-full text-left px-4 py-2 hover:bg-surface-light-secondary first:rounded-t-lg"
          >
            Rename
          </button>
          <button 
            @click.stop="handleDelete"
            class="w-full text-left px-4 py-2 hover:bg-red last:rounded-b-lg"
          >
            Delete
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import MenuIcon from '@/components/icons/MenuIcon.vue'

export default {
  name: 'FolderCard',
  components: {
    MenuIcon
  },
  props: {
    folder: {
      type: Object,
      required: true
    }
  },
  emits: ['click', 'delete', 'rename'],
  setup(props, { emit }) {
    const showMenu = ref(false)
    const menuRef = ref(null)

    const formatDate = (dateString) => {
      if (!dateString) return ''
      const date = new Date(dateString)
      return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })
    }

    const handleClickOutside = (event) => {
      if (menuRef.value && !menuRef.value.contains(event.target)) {
        showMenu.value = false
      }
    }

    onMounted(() => {
      document.addEventListener('click', handleClickOutside)
    })

    onBeforeUnmount(() => {
      document.removeEventListener('click', handleClickOutside)
    })

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
      menuRef,
      formatDate,
      handleDelete,
      handleRename
    }
  }
}
</script>
