<script>
import { ref, onMounted} from 'vue'
import User3Icon from '@/components/icons/User3Icon.vue'
import SearchIcon from '@/components/icons/SearchIcon.vue'
import HomeIcon from '@/components/icons/HomeIcon.vue'
import AssistedIcon from '@/components/icons/AssistedIcon.vue'
import PlusIcon from '@/components/icons/PlusIcon.vue'
import { driveService } from '@/utils/driveService'
import auth from '@/utils/auth'

export default {
  name: 'SidebarBase',
  components: {
    User3Icon,
    SearchIcon,
    HomeIcon,
    AssistedIcon,
    PlusIcon
  }, setup() {
    const loading = ref(false)
    const folders = ref([])
    const documents = ref([])

    const loadContent = async (folderId = null) => {
      loading.value = true
      try {
        // Load folders
        const allFolders = folders.value = await driveService.getUserFolderTree(auth.getUserId())
        
        console.log("************** FOLDERS ****************");
        console.log(allFolders);
        // Load documents - filter by current folder
        const allDocuments = await driveService.getAllDocuments()
        documents.value = allDocuments.filter(doc => doc.folderId === folderId)
        
        console.log("************** DOCUMENTS ****************");
        console.log(allDocuments);
      } catch (error) {
        console.error('Error loading content:', error)
        alert('Failed to load content. Please try again.')
      } finally {
        loading.value = false
      }
    }

    onMounted(() => {
      loadContent()
    })

    return {
        loading,
        folders,
        documents
    }
  }
}


</script>

<template>
    <div class="flex flex-col w-[250px] bg-surface-light-secondary border-r border-border-light-subtle px-3">
        <div class="h-[50px] w-full flex flex-row items-center">
            <User3Icon :size="32" class="w-8 h-8 text-accent" />
            <div class="flex flex-col w-full">
                <div class="flex flex-row justify-between w-full">
                    <h1 class=" text-surface-dark">Name</h1>
                    <img src="@/assets/icons/chevron-down.svg" alt="User" class="w-6 h-6" />
                </div>
            </div>
        </div>
        <div class="mt-6">
            <div>
                <div class="flex gap-2">
                    <SearchIcon :size="32" class="h-8 w-8 text-accent" />
                    <input type="text" class="bg-transparent border-none focus:outline-none placeholder-black" placeholder="Search" name="search">
                </div>
                <div class="flex gap-2">
                    <HomeIcon :size="32" class="w-8 h-8 text-accent" />
                    <button class="text-xl">Home</button>
                </div>
            </div>
            
            <div class="mt-6">
                <h2 class="text-[15px] mb-2">Papairs AI Settings</h2>
                <div class="text-xl flex">
                    <AssistedIcon :size="32" class="h-8 w-8 text-accent" />
                    Assisted writing
                </div>
            </div>
            
            <div class="mt-6">
                <div class="flex justify-between">
                    <h2 class="text-[15px]">Papairs</h2>
                    <button>  <PlusIcon :size="16" class="h-3 w-3 text-accent" /></button>
                </div>
                <div class="owned_papairs"></div>
            </div>
        </div>
    </div>

</template>
