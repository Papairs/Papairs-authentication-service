<script>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useTheme } from '@/composables/useTheme'
import { useAssistedWriting } from '@/composables/useAssistedWriting'
import { API_BASE_URL } from '@/config'
import User3Icon from '@/components/icons/User3Icon.vue'
import SearchIcon from '@/components/icons/SearchIcon.vue'
import HomeIconNew from '@/components/icons/HomeIconNew.vue'
import AssistedIcon from '@/components/icons/AssistedIcon.vue'
import PlusIcon from '@/components/icons/PlusIcon.vue'
import FlashcardIcon from '@/components/icons/FlashcardIcon.vue'
import FolderIcon from '@/components/icons/FolderIcon.vue'
import DocumentIcon from '@/components/icons/DocumentIcon.vue'
import ChevronDownIcon from '@/components/icons/ChevronDownIcon.vue'
import DarkModeIcon from '@/components/icons/DarkModeIcon.vue'
import SettingsIcon from '@/components/icons/SettingsIcon.vue'
import FolderTreeItem from '@/components/FolderTreeItem.vue'
import NewDropdown from '@/components/NewDropdown.vue'
import { driveService } from '@/utils/driveService'
import auth from '@/utils/auth'
import axios from 'axios'

export default {
  name: 'SidebarBase',
  components: {
    User3Icon,
    SearchIcon,
    HomeIconNew,
    AssistedIcon,
    PlusIcon,
    FlashcardIcon,
    FolderIcon,
    DocumentIcon,
    ChevronDownIcon,
    DarkModeIcon,
    SettingsIcon,
    FolderTreeItem,
    NewDropdown
  },
  setup() {
    const router = useRouter()
    const route = useRoute()
    const { isDark, toggleTheme } = useTheme()
    const { isAssistedWritingEnabled, toggleAssistedWriting } = useAssistedWriting()

    // State
    const loading = ref(false)
    const folderTree = ref([])
    const isDropdownOpen = ref(false)
    const isSettingsDropdownOpen = ref(false)
    const isNewDropdownOpen = ref(false)
    const isFolderTreeDropdownOpen = ref(false)
    const isSearchActive = ref(false)
    const searchQuery = ref('')
    const searchResults = ref([])

    // Computed
    const userDisplayName = computed(() => {
      const user = auth.getUser()
      if (user?.username) return user.username
      const userId = auth.getUserId()
      return userId ? userId.substring(0, 4) : 'User'
    })

    const isHomeActive = computed(() => route.path === '/' || route.path === '/drive' || route.path.startsWith('/drive/'))
    const isSharedActive = computed(() => route.path === '/drive/shared')
    const isNotebook = computed(() => route.path === '/' || route.path === '/drive')
    const isFlashcardsView = computed(() => route.path.startsWith('/flashcards'))

    // Navigation
    const navigateTo = (path) => router.push(path)
    
    const navigateToPage = (pageId) => {
      router.push(`/docs/${pageId}`)
      clearSearch()
    }

    // Dropdowns
    const toggleDropdown = () => {
      isDropdownOpen.value = !isDropdownOpen.value
    }

    const toggleSettingsDropdown = () => {
      isSettingsDropdownOpen.value = !isSettingsDropdownOpen.value
    }

    const toggleNewDropdown = () => {
      isNewDropdownOpen.value = !isNewDropdownOpen.value
    }

    const toggleFolderTreeDropdown = () => {
      isFolderTreeDropdownOpen.value = !isFolderTreeDropdownOpen.value
    }

    // Settings
    const handleLogout = () => {
      auth.logout()
      window.location.href = '/login'
    }

    // Modals
    const openNewDocumentModal = () => {
      isNewDropdownOpen.value = false
      window.dispatchEvent(new CustomEvent('open-create-document-modal'))
    }

    const openNewFolderModal = () => {
      isNewDropdownOpen.value = false
      window.dispatchEvent(new CustomEvent('open-create-folder-modal'))
    }

    // Folder Tree
    const loadFolderTree = async () => {
      if (!auth.getUserId()) {
        folderTree.value = []
        return
      }
      
      loading.value = true
      try {
        folderTree.value = await driveService.getUserFolderTree()
      } catch (error) {
        folderTree.value = []
        if (error.response?.status !== 401 && error.code !== 'ERR_NETWORK') {
          console.error('Error loading folder tree:', error)
        }
      } finally {
        loading.value = false
      }
    }

    // Search
    const activateSearch = () => {
      isSearchActive.value = true
      setTimeout(() => {
        document.getElementById('sidebar-search-input')?.focus()
      }, 0)
    }

    const deactivateSearch = () => {
      if (!searchQuery.value) {
        isSearchActive.value = false
        searchResults.value = []
      }
    }

    const performSearch = async () => {
      if (!searchQuery.value.trim()) {
        searchResults.value = []
        return
      }

      try {
        const headers = await auth.getAuthHeaders(router)
        const response = await axios.get(`${API_BASE_URL}/api/docs/pages`, { headers })
        const pages = response.data || []
        const query = searchQuery.value.toLowerCase().trim()
        
        searchResults.value = pages
          .filter(page => page.title.toLowerCase().includes(query))
          .map(page => ({ pageId: page.pageId, title: page.title, content: '' }))
          .slice(0, 10)
      } catch (error) {
        console.error('Search failed:', error)
        searchResults.value = []
      }
    }

    const clearSearch = () => {
      searchQuery.value = ''
      searchResults.value = []
      isSearchActive.value = false
    }

    // Lifecycle
    onMounted(() => {
      if (auth.getUserId()) {
        loadFolderTree()
      }
    })
    
    watch(() => route.path, (newPath) => {
      if (auth.getUserId() && (newPath.startsWith('/docs') || newPath.startsWith('/drive') || newPath === '/')) {
        loadFolderTree()
      }
    })

    return {
      loading,
      folderTree,
      userDisplayName,
      isDropdownOpen,
      isSettingsDropdownOpen,
      isNewDropdownOpen,
      isFolderTreeDropdownOpen,
      assistedWritingEnabled: isAssistedWritingEnabled,
      isHomeActive,
      isSharedActive,
      isNotebook,
      isFlashcardsView,
      isDark,
      isSearchActive,
      searchQuery,
      searchResults,
      toggleDropdown,
      toggleSettingsDropdown,
      toggleNewDropdown,
      toggleFolderTreeDropdown,
      toggleAssistedWriting,
      toggleTheme,
      handleLogout,
      openNewDocumentModal,
      openNewFolderModal,
      navigateTo,
      activateSearch,
      deactivateSearch,
      performSearch,
      clearSearch,
      navigateToPage
    }
  }
}


</script>

<template>
    <div class="flex flex-col w-[230px] bg-surface-light-secondary border-r border-border-light-subtle h-screen text-content-primary">
        <div class="h-[50px] px-4 flex items-center relative">
            <User3Icon :size="28" class="text-accent flex-shrink-0" />
            <div class="flex flex-col flex-1 ml-2">
                <div class="flex justify-between w-full items-center">
                    <h1 class="text-sm font-medium">{{ userDisplayName }}</h1>
                    <button @click="toggleDropdown" class="focus:outline-none">
                        <ChevronDownIcon 
                            :size="20" 
                            class-name="transition-transform"
                            :class="{ 'rotate-180': isDropdownOpen }"
                        />
                    </button>
                </div>
            </div>
            
            <!-- Dropdown Menu -->
            <div 
                v-if="isDropdownOpen" 
                class="absolute top-12 right-4 bg-surface-light content-center border border-border-dark rounded-lg shadow-lg z-10 min-w-[120px] "
            >
                <button 
                    @click="handleLogout" 
                    class="w-full text-left px-4 py-2 hover:bg-surface-light-secondary text-sm rounded-lg"
                >
                    Logout
                </button>
            </div>
        </div>
        
        <!-- Scrollable Content -->
        <div class="flex-1 overflow-y-auto">
            <div class="mt-3 flex flex-col gap-0.5 px-3">
            <!-- New Button with Dropdown -->
            <div class="relative mb-3">
                <button 
                    @click="toggleNewDropdown"
                    class="inline-flex items-center justify-center px-3 py-2 w-[50%] border border-content-primary rounded-lg hover:bg-surface-light-secondary"
                >
                    <span class="text-sm font-semibold">New</span>
                    <span class="text-base text-accent font-bold leading-none ml-0.5 -mt-1.5">+</span>
                </button>
                
                <NewDropdown 
                    :is-open="isNewDropdownOpen"
                    @new-papair="openNewDocumentModal"
                    @new-folder="openNewFolderModal"
                    @close="isNewDropdownOpen = false"
                />
            </div>
            
            <!-- Search -->
            <div 
                v-if="!isSearchActive"
                @click="activateSearch"
                class="flex gap-3 items-center px-2 py-2 rounded cursor-pointer hover:bg-surface-light-secondary"
            >
                <SearchIcon :size="24" />
                <span class="text-base">Search</span>
            </div>
            <div 
                v-else
                class="flex gap-3 items-center px-2 py-2 rounded bg-surface-light-secondary relative">
                <SearchIcon :size="20" class="text-content-primary flex-shrink-0" />
                <input
                    id="sidebar-search-input"
                    v-model="searchQuery"
                    @input="performSearch"
                    @blur="deactivateSearch"
                    type="text"
                    placeholder="Search..."
                    class="flex-1 bg-transparent outline-none text-base w-full"
                />
                <button 
                    v-if="searchQuery"
                    @mousedown.prevent="clearSearch"
                    class="text-content-secondary hover:text-content-primary"
                >
                    ✕
                </button>
            </div>
            
            <!-- Search Results Dropdown -->
            <div 
                v-if="isSearchActive && searchResults.length > 0" 
                class="bg-surface-light border border-border-light rounded-lg shadow-lg mt-1 max-h-48 overflow-y-auto mx-2 relative z-50"
            >
                <button
                    v-for="result in searchResults"
                    :key="result.pageId"
                    @mousedown.prevent="navigateToPage(result.pageId)"
                    class="w-full text-left px-3 py-2 hover:bg-surface-light-secondary flex items-start gap-2 border-b border-border-light-subtle last:border-0"
                >
                    <DocumentIcon :size="14" class="mt-1 flex-shrink-0" />
                    <div class="flex-1 min-w-0">
                        <div class="text-sm font-medium truncate">{{ result.title }}</div>
                    </div>
                </button>
            </div>
            
            <!-- No Results Message -->
            <div 
                v-if="isSearchActive && searchQuery && searchResults.length === 0" 
                class="bg-surface-light border border-border-light rounded-lg shadow-lg mt-1 mx-2 px-3 py-2 text-sm text-content-secondary text-center"
            >
                No results found
            </div>
            
            <!-- Home -->
            <a 
                href="/drive" 
                class="flex gap-3 items-center px-2 py-2 rounded"
            >
                <HomeIconNew :size="24" :class="isHomeActive ? 'text-accent' : ''" />
                <span class="text-base" :class="isHomeActive ? 'text-accent font-medium' : ''">Home</span>
            </a>
            
            <!-- Flashcards -->
            <button
                @click="navigateTo('/flashcards')"
                class="flex gap-3 items-center px-2 py-2 rounded"
            >
                <FlashcardIcon :size="24" :class="isFlashcardsView ? 'text-accent' : ''" />
                <span class="text-base" :class="isFlashcardsView ? 'text-accent font-medium' : ''">Flashcards</span>
            </button>
            
            <!-- My papairs Section -->
            <div class="mt-3 mb-1">
                <h2 class="text-xs font-semibold text-content-primary px-2">My papairs</h2>
            </div>
            
            <!-- My notebook -->
            <a 
                href="/drive" 
                class="flex gap-3 items-center px-2 py-2 rounded"
            >
                <FolderIcon :size="20" :class="isNotebook ? 'text-accent' : ''" />
                <span class="text-base" :class="isNotebook ? 'text-accent font-medium' : ''">My notebook</span>
            </a>
            
            <!-- Shared with me -->
            <a 
                href="/drive/shared" 
                class="flex gap-3 items-center px-2 py-2 rounded"
                :class="isSharedActive ? 'bg-surface-light-secondary' : 'hover:bg-surface-light-secondary'"
            >
                <svg class="w-5 h-5" :class="isSharedActive ? 'text-accent' : ''" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
                </svg>
                <span class="text-base" :class="isSharedActive ? 'text-accent font-medium' : ''">Shared with me</span>
            </a>
            
            <!-- Papairs Section -->
            <div class="mt-3 mb-1">
                <div class="flex justify-between items-center px-2 relative">
                    <h2 class="text-xs font-semibold text-content-primary">Papairs</h2>
                    <button @click="toggleFolderTreeDropdown" class="text-accent hover:text-orange-600">
                        <PlusIcon :size="20" />
                    </button>
                    
                    <NewDropdown 
                        :is-open="isFolderTreeDropdownOpen"
                        @new-papair="openNewDocumentModal"
                        @new-folder="openNewFolderModal"
                        @close="isFolderTreeDropdownOpen = false"
                    />
                </div>
            </div>
            
            <!-- Folder Tree -->
            <div class="folder-tree-container">
                <div v-if="loading" class="text-sm text-content-secondary px-2">Loading...</div>
                <div v-else-if="folderTree.length === 0" class="text-sm text-content-secondary px-2">No folders yet</div>
                <div v-else>
                    <FolderTreeItem
                        v-for="folder in folderTree"
                        :key="folder.folderId"
                        :folder="folder"
                        :level="0"
                    />
                </div>
            </div>
        </div>
        </div>
        
        <!-- Bottom Settings Section -->
        <div class="px-3 py-1.5">
            <!-- Version -->
            <div class="mb-2 px-2">
                <p class="text-xs text-accent font-medium">Papairs v1.0</p>
            </div>
            
            <div class="flex flex-col gap-2 pb-2">
                <!-- Settings with Dropdown -->
                <div class="relative">
                    <button 
                        @click="toggleSettingsDropdown"
                        class="w-full flex gap-3 items-center px-2 hover:bg-surface-light-secondary rounded"
                    >
                        <SettingsIcon :size="20" class-name="text-content-primary" />
                        <span class="text-sm text-left flex-1">Settings</span>
                        <ChevronDownIcon 
                            :size="20" 
                            class="transition-transform text-content-primary"
                            :class="{ 'rotate-180': isSettingsDropdownOpen }"
                        />
                    </button>
                    
                    <!-- Settings Dropdown -->
                    <div 
                        v-if="isSettingsDropdownOpen"
                        class="mt-1 ml-4 pl-3 border-l-2 border-border-light space-y-0.5"
                    >
                        <!-- Assisted Writing -->
                        <div class="flex gap-2 items-center px-2 py-1.5 hover:bg-surface-light-secondary rounded cursor-pointer" @click="toggleAssistedWriting">
                            <AssistedIcon :size="20" class="text-accent flex-shrink-0" />
                            <span class="text-sm flex-1">Assisted Writing</span>
                            <div 
                                class="w-8 h-4 rounded-full relative transition-colors duration-200 cursor-pointer flex-shrink-0"
                                :class="assistedWritingEnabled ? 'bg-accent' : 'bg-content-secondary'"
                            >
                                <div 
                                    class="absolute top-0.5 w-3 h-3 bg-white rounded-full transition-all duration-200"
                                    :class="assistedWritingEnabled ? 'right-0.5' : 'left-0.5'"
                                ></div>
                            </div>
                        </div>
                    </div>
                </div>
                
                <!-- Dark Mode -->
                <button @click="toggleTheme" class="flex gap-3 items-center px-2 hover:bg-surface-light-secondary rounded">
                    <DarkModeIcon :size="20" class="text-content-primary" />
                    <span class="text-sm">Dark Mode</span>
                </button>
            </div>
        </div>
    </div>
</template>
