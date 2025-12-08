<script>
import FlashcardCard from './FlashcardCard.vue'

export default {
  name: 'FlashcardsGrid',
  components: {
    FlashcardCard
  },
  props: {
    flashcards: {
      type: Array,
      default: () => []
    },
    loading: {
      type: Boolean,
      default: false
    },
    flippedCards: {
      type: Set,
      default: () => new Set()
    },
    filteredFlashcards: {
      type: Array,
      default: () => []
    },
    getPageTitle: {
      type: Function,
      required: true
    }
  },
  emits: ['toggle-flip', 'delete']
}
</script>

<template>
  <div class="flex-1 overflow-y-auto p-8">
    <div v-if="loading" class="text-center py-8 text-content-secondary">
      Loading flashcards...
    </div>
    
    <div v-else-if="flashcards.length === 0" class="text-center py-8">
      <p class="text-content-secondary text-lg mb-4">No flashcards yet!</p>
      <p class="text-content-secondary">Generate flashcards from your documents to get started.</p>
    </div>

    <div v-else-if="filteredFlashcards.length === 0" class="text-center py-8">
      <p class="text-content-secondary text-lg mb-4">No flashcards for this document</p>
      <p class="text-content-secondary">Try selecting a different document or view all flashcards.</p>
    </div>
    
    <!-- Flashcards Grid -->
    <div v-else class="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
      <FlashcardCard
        v-for="card in filteredFlashcards"
        :key="card.flashcardId"
        :card="card"
        :is-flipped="flippedCards.has(card.flashcardId)"
        :page-title="getPageTitle(card.pageId)"
        @toggle-flip="$emit('toggle-flip', $event)"
        @delete="$emit('delete', $event)"
      />
    </div>
  </div>
</template>
