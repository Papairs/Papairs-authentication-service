<script>
import { ref, computed, onMounted} from 'vue'
import User3Icon from '@/components/icons/User3Icon.vue'
import SearchIcon from '@/components/icons/SearchIcon.vue'
import HomeIcon from '@/components/icons/HomeIcon.vue'
import AssistedIcon from '@/components/icons/AssistedIcon.vue'
import PlusIcon from '@/components/icons/PlusIcon.vue'
import FolderTreeItem from '@/components/FolderTreeItem.vue'
import { driveService } from '@/utils/driveService'
import auth from '@/utils/auth'

export default {
  name: 'SidebarBase',
  components: {
    User3Icon,
    SearchIcon,
    HomeIcon,
    AssistedIcon,
    PlusIcon,
    FolderTreeItem
  },
  setup() {
    const loading = ref(false)
    const folderTree = ref([])
    const isDropdownOpen = ref(false)
    const assistedWritingEnabled = ref(false)

    const userDisplayName = computed(() => {
      const user = auth.getUser()
      // Check if username exists
      if (user?.username) {
        return user.username
      }
      // Otherwise use first 4 characters of userId
      const userId = auth.getUserId()
      return userId ? userId.substring(0, 4) : 'User'
    })

    const toggleDropdown = () => {
      isDropdownOpen.value = !isDropdownOpen.value
    }

    const toggleAssistedWriting = () => {
      assistedWritingEnabled.value = !assistedWritingEnabled.value
      console.log('Assisted Writing:', assistedWritingEnabled.value ? 'Enabled' : 'Disabled')
    }

    const handleLogout = () => {
      auth.logout()
      window.location.href = '/login'
    }

    const loadFolderTree = async () => {
      loading.value = true
      try {
        folderTree.value = await driveService.getUserFolderTree(auth.getUserId())
        
        console.log("************** FOLDER TREE ****************")
        console.log(folderTree.value)
      } catch (error) {
        console.error('Error loading folder tree:', error)
        alert('Failed to load folder tree. Please try again.')
      } finally {
        loading.value = false
      }
    }

    onMounted(() => {
      loadFolderTree()
    })

    return {
        loading,
        folderTree,
        userDisplayName,
        isDropdownOpen,
        assistedWritingEnabled,
        toggleDropdown,
        toggleAssistedWriting,
        handleLogout
    }
  }
}


</script>

<template>
    <div class="flex flex-col w-[280px] bg-white border-r border-gray-200 h-screen overflow-y-auto">
        <div class="px-4 py-3 flex flex-row items-center relative border-b border-gray-100">
            <User3Icon :size="32" class="w-8 h-8 text-accent flex-shrink-0" />
            <div class="flex flex-col flex-1 ml-2">
                <div class="flex flex-row justify-between w-full items-center">
                    <h1 class="text-sm font-medium text-gray-800">{{ userDisplayName }}</h1>
                    <button @click="toggleDropdown" class="focus:outline-none p-1">
                        <img 
                            src="@/assets/icons/chevron-down.svg" 
                            alt="Menu" 
                            class="w-5 h-5 transition-transform"
                            :class="{ 'rotate-180': isDropdownOpen }"
                        />
                    </button>
                </div>
            </div>
            
            <!-- Dropdown Menu -->
            <div 
                v-if="isDropdownOpen" 
                class="absolute top-14 right-4 bg-white border border-gray-200 rounded-lg shadow-lg z-10 min-w-[120px]"
            >
                <button 
                    @click="handleLogout" 
                    class="w-full text-left px-4 py-2 hover:bg-gray-50 text-sm rounded-lg"
                >
                    Logout
                </button>
            </div>
        </div>
        <div class="mt-4 flex flex-col gap-1">
            <!-- Search -->
            <div class="flex gap-3 items-center px-2 py-2 hover:bg-gray-100 rounded cursor-pointer">
                <SearchIcon :size="24" class="w-6 h-6 text-gray-600" />
                <span class="text-base text-gray-700">Search</span>
            </div>
            
            <!-- Home -->
            <a href="/drive" class="flex gap-3 items-center px-2 py-2 hover:bg-gray-100 rounded">
                <HomeIcon :size="24" class="w-6 h-6 text-gray-600" />
                <span class="text-base text-gray-700">Home</span>
            </a>
            
            <!-- Papairs AI Settings Section -->
            <div class="mt-4 mb-2">
                <h2 class="text-xs font-semibold text-gray-600 px-2">Papairs AI Settings</h2>
            </div>
            
            <!-- Assisted Writing -->
            <div class="flex gap-3 items-center px-2 py-2 hover:bg-gray-100 rounded cursor-pointer" @click="toggleAssistedWriting">
                <AssistedIcon :size="24" class="w-6 h-6 text-accent" />
                <span class="text-base text-gray-700 flex-1">Assisted Writing</span>
                <div 
                    class="w-10 h-5 rounded-full relative transition-colors duration-200 cursor-pointer"
                    :class="assistedWritingEnabled ? 'bg-accent' : 'bg-gray-300'"
                >
                    <div 
                        class="absolute top-0.5 w-4 h-4 bg-white rounded-full transition-all duration-200"
                        :class="assistedWritingEnabled ? 'right-0.5' : 'left-0.5'"
                    ></div>
                </div>
            </div>
            
            <!-- Papairs Section -->
            <div class="mt-6 mb-2">
                <div class="flex justify-between items-center px-2">
                    <h2 class="text-xs font-semibold text-gray-600">Papairs</h2>
                    <button class="text-accent hover:text-orange-600">
                        <PlusIcon :size="20" class="w-5 h-5" />
                    </button>
                </div>
            </div>
            
            <!-- Folder Tree -->
            <div class="folder-tree-container">
                <div v-if="loading" class="text-sm text-gray-500 px-2">Loading...</div>
                <div v-else-if="folderTree.length === 0" class="text-sm text-gray-500 px-2">No folders yet</div>
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

</template>
