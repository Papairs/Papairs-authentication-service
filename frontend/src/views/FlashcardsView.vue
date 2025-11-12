<script>
import SidebarBase from '@/components/SidebarBase.vue'
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import auth from '@/utils/auth'
import axios from 'axios'

export default {
  name: 'FlashcardsView',
  components: { SidebarBase },
  setup() {
    const router = useRouter()
    const flashcards = ref([])
    const loading = ref(false)
    const flippedCards = ref(new Set())

    async function loadFlashcards() {
      loading.value = true
      try {
        const headers = await auth.getAuthHeaders(router)
        const response = await axios.get('http://localhost:8082/api/docs/flashcards', {
          headers
        })
        
        if (response.data.success) {
          flashcards.value = response.data.data || []
        }
      } catch (error) {
        console.error('Failed to load flashcards:', error)
        flashcards.value = []
      } finally {
        loading.value = false
      }
    }

    async function deleteFlashcard(flashcardId) {
      if (!confirm('Are you sure you want to delete this flashcard?')) {
        return
      }

      try {
        const headers = await auth.getAuthHeaders(router)
        await axios.delete(`http://localhost:8082/api/docs/flashcards/${flashcardId}`, {
          headers
        })
        
        await loadFlashcards()
      } catch (error) {
        console.error('Failed to delete flashcard:', error)
        alert('Failed to delete flashcard')
      }
    }

    function toggleFlip(flashcardId) {
      if (flippedCards.value.has(flashcardId)) {
        flippedCards.value.delete(flashcardId)
      } else {
        flippedCards.value.add(flashcardId)
      }
      // Force reactivity update
      flippedCards.value = new Set(flippedCards.value)
    }

    function isFlipped(flashcardId) {
      return flippedCards.value.has(flashcardId)
    }

    onMounted(() => {
      loadFlashcards()
    })

    return {
      flashcards,
      loading,
      deleteFlashcard,
      toggleFlip,
      isFlipped
    }
  }
}
</script>

<template>
  <div class="flex flex-row h-screen w-screen bg-surface-light overflow-hidden">
    <SidebarBase />
    <div class="flex flex-col h-full w-full overflow-hidden">
      <!-- Header -->
      <div class="flex flex-row h-[50px] w-full border-b-2 border-accent flex-shrink-0 items-center px-6">
        <h1 class="text-2xl font-bold text-content-primary">My Flashcards</h1>
      </div>

      <!-- Content -->
      <div class="flex-1 overflow-y-auto p-8">
        <div v-if="loading" class="text-center py-8 text-content-secondary">
          Loading flashcards...
        </div>
        
        <div v-else-if="flashcards.length === 0" class="text-center py-8">
          <p class="text-content-secondary text-lg mb-4">No flashcards yet!</p>
          <p class="text-content-secondary">Generate flashcards from your documents to get started.</p>
        </div>
        
        <!-- Flashcards Grid -->
        <div v-else class="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
          <div
            v-for="card in flashcards"
            :key="card.flashcardId"
            class="bg-white border border-border-light-subtle rounded-lg p-6 hover:shadow-lg transition-shadow cursor-pointer"
            @click="toggleFlip(card.flashcardId)"
          >
            <!-- Card Header -->
            <div class="flex justify-between items-start mb-4">
              <span 
                class="px-2 py-1 text-xs font-medium rounded"
                :class="{
                  'bg-green-100 text-green-800': card.difficultyLevel === 'EASY',
                  'bg-yellow-100 text-yellow-800': card.difficultyLevel === 'MEDIUM',
                  'bg-red-100 text-red-800': card.difficultyLevel === 'HARD'
                }"
              >
                {{ card.difficultyLevel }}
              </span>
              
              <button 
                @click.stop="deleteFlashcard(card.flashcardId)"
                class="text-content-secondary hover:text-red transition-colors"
                title="Delete flashcard"
              >
                🗑️
              </button>
            </div>

            <!-- Card Content -->
            <div class="min-h-[120px] flex flex-col justify-center">
              <div v-if="!isFlipped(card.flashcardId)">
                <h3 class="text-sm text-content-secondary mb-2">Question:</h3>
                <p class="text-content-primary font-medium">{{ card.question }}</p>
              </div>
              
              <div v-else>
                <h3 class="text-sm text-content-secondary mb-2">Answer:</h3>
                <p class="text-content-primary">{{ card.answer }}</p>
              </div>
            </div>

            <!-- Card Footer -->
            <div class="mt-4 pt-4 border-t border-border-light-subtle text-center">
              <span class="text-sm text-content-secondary">
                {{ isFlipped(card.flashcardId) ? 'Click to see question' : 'Click to see answer' }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
