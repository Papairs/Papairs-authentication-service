<template>
    <div class="flex flex-col w-[250px] bg-surface-light-secondary border-r border-border-light-subtle">
        <div class="h-[50px] w-full flex flex-row items-center px-3">
            <User3Icon :size="32" class="w-8 h-8 text-accent" />
            <div class="flex flex-col w-full">
                <div class="flex flex-row justify-between w-full">
                    <h1 class=" text-surface-dark">Name</h1>
                    <img src="@/assets/icons/chevron-down.svg" alt="User" class="w-6 h-6" />
                </div>
            </div>
            
        </div>
    </div>

</template>

<script>
import User3Icon from '@/components/icons/User3Icon.vue'

export default {
  name: 'SidebarBase',
  components: {
    User3Icon
  }
}
</script>