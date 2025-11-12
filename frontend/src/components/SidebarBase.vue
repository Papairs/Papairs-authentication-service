<script>
import User3Icon from '@/components/icons/User3Icon.vue'
import { useRouter } from 'vue-router'

export default {
  name: 'SidebarBase',
  components: {
    User3Icon
  },
  setup() {
    const router = useRouter()
    
    function navigateTo(route) {
      router.push(route)
    }
    
    return {
      navigateTo
    }
  }
}
</script>

<template>
    <div class="flex flex-col w-[250px] bg-surface-light-secondary border-r border-border-light-subtle">
        <!-- User Section -->
        <div class="h-[50px] w-full flex flex-row items-center px-3 border-b border-border-light-subtle">
            <User3Icon :size="32" class="w-8 h-8 text-accent" />
            <div class="flex flex-col w-full">
                <div class="flex flex-row justify-between w-full">
                    <h1 class="text-surface-dark">Name</h1>
                    <img src="@/assets/icons/chevron-down.svg" alt="User" class="w-6 h-6" />
                </div>
            </div>
        </div>

        <!-- Navigation Links -->
        <nav class="flex flex-col p-2 space-y-1">
            <button
                @click="navigateTo('/drive')"
                class="flex items-center gap-3 px-3 py-2 text-content-primary hover:bg-surface-light rounded-md transition-colors text-left"
            >
                <span>📁</span>
                <span>Drive</span>
            </button>
            
            <button
                @click="navigateTo('/docs')"
                class="flex items-center gap-3 px-3 py-2 text-content-primary hover:bg-surface-light rounded-md transition-colors text-left"
            >
                <span>📝</span>
                <span>Documents</span>
            </button>
            
            <button
                @click="navigateTo('/flashcards')"
                class="flex items-center gap-3 px-3 py-2 text-content-primary hover:bg-surface-light rounded-md transition-colors text-left"
            >
                <span>🎴</span>
                <span>Flashcards</span>
            </button>
        </nav>
    </div>
</template>
