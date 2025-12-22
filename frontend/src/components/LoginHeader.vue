<script>
import GlobeIcon from '@/components/icons/GlobeIcon.vue';

export default {
  name: 'LoginHeader',
  components: {
    GlobeIcon
  }
}
</script>

<template>
  <div>
    <header class="h-16 bg-surface-light flex items-center px-8">
      <div class="flex items-center gap-3">
        <img src="@/assets/icons/papairslogo.svg" alt="Papairs Logo" class="h-8 transition-all duration-300 dark:invert dark:brightness-[0.93] dark:contrast-[0.92]" />
        <h1 class="text-xl font-bold text-content-primary">
          pap<span class="text-accent">AI</span>rs
        </h1>
      </div>
      
      <div class="flex items-center gap-2 text-sm text-content-primary ml-8">
        <GlobeIcon :size="16" class-name="text-content-primary opacity-60" />
        <span class="text-content-primary opacity-60">English</span>
        <span class="text-content-primary opacity-60">▾</span>
      </div>
    </header>
    <div class="h-0.5 bg-accent"></div>
  </div>
</template>