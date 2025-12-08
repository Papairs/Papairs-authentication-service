<template>
  <div 
    class="group relative bg-surface-light border border-content-primary rounded-xl hover:shadow-md transition-all cursor-pointer overflow-visible"
    style="aspect-ratio: 1; width: 100%"
    @click="$emit('click')"
  >
    <!-- Header with Icon, Title and Menu on same line -->
    <div class="flex items-center justify-between p-4">
      <!-- Icon and Title -->
      <div class="flex items-center space-x-3 flex-1 min-w-0">
        <!-- Document Icon -->
        <div class="flex-shrink-0">
          <svg class="w-5 h-5 text-content-primary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
          </svg>
        </div>
        
        <!-- Document Title -->
        <h3 class="text-sm font-medium text-content-primary truncate">
          {{ document.title }}
        </h3>
      </div>
      
      <!-- Menu Button Container - Only show if user has EDITOR role or is owner -->
      <div v-if="canManageDocument" class="relative" ref="menuRef">
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
            @click.stop="handleShare"
            class="w-full text-left px-4 py-2 hover:bg-surface-light-secondary"
          >
            Share
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

    <!-- Whitespace area for visual breathing room -->
    <div class="flex-1"></div>
  </div>
</template>

<script>
import { ref, onMounted, onBeforeUnmount, computed } from 'vue'
import MenuIcon from '@/components/icons/MenuIcon.vue'
import auth from '@/utils/auth'

export default {
  name: 'DocumentCard',
  components: {
    MenuIcon
  },
  props: {
    document: {
      type: Object,
      required: true
    }
  },
  emits: ['click', 'delete', 'rename', 'share'],
  setup(props, { emit }) {
    const showMenu = ref(false)
    const menuRef = ref(null)
    const currentUserId = auth.getUserId()

    // Check if user can manage document (is owner or has EDITOR role)
    const canManageDocument = computed(() => {
      // Owner can always manage
      if (currentUserId === props.document.ownerId) {
        return true
      }
      
      // Only EDITOR and OWNER roles can manage (not VIEWER)
      return props.document.userRole === 'EDITOR' || props.document.userRole === 'OWNER'
    })

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

    const handleShare = () => {
      showMenu.value = false
      emit('share')
    }

    return {
      showMenu,
      menuRef,
      canManageDocument,
      formatDate,
      handleDelete,
      handleRename,
      handleShare
    }
  }
}
</script>
