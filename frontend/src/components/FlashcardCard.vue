<script>
import PageTitleIcon from '@/components/icons/PageTitleIcon.vue'
import TrashIcon from '@/components/icons/TrashIcon.vue'

export default {
  name: 'FlashcardCard',
  components: {
    PageTitleIcon,
    TrashIcon
  },
  props: {
    card: {
      type: Object,
      required: true
    },
    isFlipped: {
      type: Boolean,
      default: false
    },
    pageTitle: {
      type: String,
      default: ''
    }
  },
  emits: ['toggle-flip', 'delete']
}
</script>

<template>
  <div
    class="bg-surface-light border border-border-opa rounded-lg p-6 hover:shadow-lg transition-shadow cursor-pointer"
    @click="$emit('toggle-flip', card.flashcardId)"
  >
    <!-- Card Header -->
    <div class="flex justify-between items-start mb-4">
      <span 
        class="flex items-center gap-2 text-xs px-2 py-1 bg-surface-light-primary text-content-primary rounded border border-border-opa truncate max-w-[200px]" 
        :title="pageTitle"
      >
        <PageTitleIcon :size="14" class="flex-shrink-0" />
        <span class="truncate">{{ pageTitle }}</span>
      </span>
      <button 
        @click.stop="$emit('delete', card.flashcardId)"
        class="text-content-primary hover:text-red transition-colors"
        title="Delete flashcard"
      >
        <TrashIcon :size="16" />
      </button>
    </div>

    <!-- Card Content -->
    <div class="min-h-[120px] flex flex-col justify-center">
      <div v-if="!isFlipped">
        <h3 class="text-sm text-content-primary mb-2">Question:</h3>
        <p class="text-content-primary font-medium">{{ card.question }}</p>
      </div>
      
      <div v-else>
        <h3 class="text-sm text-content-primary mb-2">Answer:</h3>
        <p class="text-content-primary">{{ card.answer }}</p>
      </div>
    </div>

    <!-- Card Footer -->
    <div class="mt-4 pt-4 border-t border-border-opa text-center">
      <span class="text-sm text-content-primary">
        {{ isFlipped ? 'Click to see question' : 'Click to see answer' }}
      </span>
    </div>
  </div>
</template>
