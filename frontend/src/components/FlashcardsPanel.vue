<template>
  <transition name="slide">
    <div v-if="show" class="absolute right-0 top-0 h-full w-80 bg-surface-light border-l-2 border-border-opa overflow-y-auto">
      <div class="sticky top-0 bg-surface-light border-b border-border-opa p-4 flex justify-between items-center">
        <h3 class="font-semibold text-content-primary">Page Flashcards</h3>
        <button @click="$emit('close')" class="text-content-secondary hover:text-content-primary transition-colors">✕</button>
      </div>
      
      <div v-if="isLoading" class="p-6 text-center text-content-secondary">Loading flashcards...</div>
      <div v-else-if="flashcards.length === 0" class="p-6 text-center text-content-secondary">
        <p class="mb-2">No flashcards yet</p>
        <p class="text-sm">Generate some flashcards from your document!</p>
      </div>
      <div v-else class="p-4 space-y-3">
        <div v-for="card in flashcards" :key="card.flashcardId" class="bg-surface-light-secondary border border-border-light rounded-lg p-4 hover:shadow-md transition-shadow">
          <div class="flex justify-end mb-2">
            <button @click="$emit('delete', card.flashcardId)" class="text-xs text-content-secondary hover:text-red-600 transition-colors" title="Delete flashcard">
              <TrashIcon :size="16" />
            </button>
          </div>
          <div class="mb-3">
            <p class="text-xs text-content-secondary font-medium mb-1">Question:</p>
            <p class="text-xs text-content-primary">{{ card.question }}</p>
          </div>
          <div>
            <p class="text-xs text-content-secondary font-medium mb-1">Answer:</p>
            <p class="text-xs text-content-primary">{{ card.answer }}</p>
          </div>
          <div v-if="card.tags && card.tags.length > 0" class="mt-3 flex flex-wrap gap-1">
            <span v-for="tag in card.tags" :key="tag" class="text-xs px-2 py-1 bg-accent/10 text-accent rounded">{{ tag }}</span>
          </div>
        </div>
      </div>
    </div>
  </transition>
</template>

<script>
import TrashIcon from '@/components/icons/TrashIcon.vue'

export default {
  name: 'FlashcardsPanel',
  components: {
    TrashIcon
  },
  props: {
    show: {
      type: Boolean,
      required: true
    },
    flashcards: {
      type: Array,
      default: () => []
    },
    isLoading: {
      type: Boolean,
      default: false
    }
  },
  emits: ['close', 'delete']
}
</script>

<style scoped>
.slide-enter-active, .slide-leave-active {
  transition: transform 0.3s ease-out;
}
.slide-enter-from {
  transform: translateX(100%);
}
.slide-leave-to {
  transform: translateX(100%);
}
</style>
