<template>
  <div class="min-h-screen bg-surface-light py-8">
    <div class="max-w-4xl mx-auto px-4">
      <!-- Header -->
      <div class="mb-8">
        <div class="flex items-center justify-between mb-4">
          <h1 class="text-3xl font-bold text-content-primary">Study Mode</h1>
          <button
            @click="exitStudyMode"
            class="px-4 py-2 text-content-secondary hover:text-content-primary transition-colors"
          >
            ← Back to Flashcards
          </button>
        </div>

        <!-- Progress Bar -->
        <div class="bg-surface-light border border-border-opa rounded-lg shadow-sm p-4">
          <div class="flex items-center justify-between mb-2">
            <span class="text-sm font-medium text-content-primary">
              Progress: {{ completedCount }} / {{ totalCards }}
            </span>
            <span class="text-sm text-content-secondary">
              {{ remainingCount }} cards remaining
            </span>
          </div>
          <div class="w-full bg-surface-light-secondary rounded-full h-2">
            <div
              class="bg-accent h-2 rounded-full transition-all duration-300"
              :style="{ width: progressPercentage + '%' }"
            ></div>
          </div>
        </div>
      </div>

      <!-- Study Complete Message -->
      <div v-if="studyComplete" class="bg-surface-light border border-border-opa rounded-lg shadow-lg p-8 text-center">
        <!-- Circular Progress -->
        <div class="flex justify-center mb-6">
          <CircularProgress :current="learnedCount" :total="totalFlashcards" :size="160" :stroke-width="12" />
        </div>
        
        <h2 class="text-2xl font-bold text-content-primary mb-2">Study Session Complete!</h2>
        <p class="text-content-secondary mb-6">
          You've reviewed all {{ totalCards }} flashcard{{ totalCards !== 1 ? 's' : '' }}!
        </p>
        <div class="flex gap-4 justify-center flex-wrap">
          <button
            @click="restartStudy"
            class="px-6 py-3 bg-accent text-content-white rounded-lg hover:bg-[#E66900] transition-colors"
          >
            Study Unlearned Cards
          </button>
          <button
            @click="resetAllAndRestart"
            class="px-6 py-3 border-2 border-border-opa text-content-primary rounded-lg hover:bg-surface-light-secondary transition-colors"
          >
            Study Again
          </button>
          <button
            @click="exitStudyMode"
            class="px-6 py-3 bg-surface-light-secondary text-content-primary rounded-lg hover:bg-surface-light transition-colors"
          >
            Back to Flashcards
          </button>
        </div>
      </div>

      <!-- Flashcard -->
      <div v-else-if="currentCard" class="perspective-1000">
        <div
          class="relative w-full h-96 cursor-pointer"
          @click="flipCard"
          :class="{ 'rotate-y-180': isFlipped }"
          style="transform-style: preserve-3d; transition: transform 0.6s"
        >
          <!-- Front of Card (Question) -->
          <div
            class="absolute inset-0 bg-surface-light border border-border-opa rounded-lg shadow-xl p-8 flex flex-col justify-center items-center backface-hidden"
            style="backface-visibility: hidden"
          >
            <div class="text-sm text-content-secondary mb-4">Question</div>
            <div class="text-2xl font-semibold text-content-primary text-center">
              {{ currentCard.question }}
            </div>
            <div class="mt-6 text-sm text-content-secondary">Click to flip</div>
          </div>

          <!-- Back of Card (Answer) -->
          <div
            class="absolute inset-0 bg-surface-light-secondary border border-border-opa rounded-lg shadow-xl p-8 flex flex-col justify-center items-center backface-hidden rotate-y-180"
            style="backface-visibility: hidden; transform: rotateY(180deg)"
          >
            <div class="text-sm text-accent mb-4">Answer</div>
            <div class="text-2xl font-semibold text-content-primary text-center">
              {{ currentCard.answer }}
            </div>
            <div class="mt-6 text-sm text-content-secondary">Click to flip back</div>
          </div>
        </div>

        <!-- Tags -->
        <div v-if="currentCard.tags && currentCard.tags.length > 0" class="flex flex-wrap gap-2 mt-4 justify-center">
          <span
            v-for="tag in currentCard.tags"
            :key="tag"
            class="px-3 py-1 bg-surface-light-secondary text-content-primary text-sm rounded-full"
          >
            #{{ tag }}
          </span>
        </div>

        <!-- Action Buttons -->
        <div class="flex gap-4 mt-8 justify-center">
          <button
            @click="markAsNotLearned"
            :disabled="processingAction"
            class="px-8 py-4 bg-[#FEE2E2] text-[#DC2626] rounded-lg hover:bg-[#FECACA] transition-colors font-medium flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            <XmarkIcon :size="20" />
            {{ processingAction ? 'Processing...' : 'Not Learned' }}
          </button>
          <button
            @click="markAsLearned"
            :disabled="processingAction"
            class="px-8 py-4 bg-[#D1FAE5] text-[#059669] rounded-lg hover:bg-[#A7F3D0] transition-colors font-medium flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            <CheckmarkIcon :size="20" class="text-[#059669]" />
            {{ processingAction ? 'Processing...' : 'Learned' }}
          </button>
        </div>

        <!-- Skip Button -->
        <div class="flex justify-center mt-4">
          <button
            @click="skipCard"
            :disabled="processingAction"
            class="px-4 py-2 text-content-secondary hover:text-content-primary transition-colors text-sm disabled:opacity-50 disabled:cursor-not-allowed"
          >
            Skip for now →
          </button>
        </div>
      </div>

      <!-- Loading State -->
      <div v-else-if="loading" class="bg-surface-light border border-border-opa rounded-lg shadow-lg p-8 text-center">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-accent mx-auto"></div>
        <p class="mt-4 text-content-secondary">Loading flashcards...</p>
      </div>

      <!-- No Cards Message -->
      <div v-else class="bg-surface-light border border-border-opa rounded-lg shadow-lg p-8 text-center">
        <div class="text-6xl mb-4">📚</div>
        <h2 class="text-2xl font-bold text-content-primary mb-2">No Cards to Study</h2>
        <p class="text-content-secondary mb-6">
          All your flashcards are marked as learned, or you don't have any flashcards yet.
        </p>
        <button
          @click="exitStudyMode"
          class="px-6 py-3 bg-accent text-content-white rounded-lg hover:bg-[#E66900] transition-colors"
        >
          Back to Flashcards
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { API_BASE_URL } from '@/config'
import axios from 'axios'
import auth from '../utils/auth'
import CheckmarkIcon from '@/components/icons/CheckmarkIcon.vue'
import XmarkIcon from '@/components/icons/XmarkIcon.vue'
import CircularProgress from '@/components/CircularProgress.vue'

const router = useRouter()
const route = useRoute()

const flashcards = ref([])
const allFlashcards = ref([])
const currentIndex = ref(0)
const isFlipped = ref(false)
const loading = ref(true)
const studyComplete = ref(false)
const processingAction = ref(false) // Prevent double-clicks

const currentCard = computed(() => {
  if (currentIndex.value < flashcards.value.length) {
    return flashcards.value[currentIndex.value]
  }
  return null
})

const totalCards = computed(() => flashcards.value.length)
const completedCount = computed(() => currentIndex.value)
const remainingCount = computed(() => totalCards.value - completedCount.value)
const progressPercentage = computed(() => {
  if (totalCards.value === 0) return 0
  return (completedCount.value / totalCards.value) * 100
})

// Total flashcards and learned count for completion screen
const totalFlashcards = computed(() => allFlashcards.value.length)
const learnedCount = computed(() => allFlashcards.value.filter(card => card.learned).length)

onMounted(async () => {
  await loadFlashcards()
})

async function loadFlashcards() {
  try {
    loading.value = true
    const headers = await auth.getAuthHeaders(router)
    
    // Get filter from query params (passed from FlashcardsView)
    const pageId = route.query.pageId
    
    let response
    if (pageId && pageId !== 'all') {
      // Load flashcards for specific page
      response = await axios.get(
        `${API_BASE_URL}/api/docs/flashcards/page/${pageId}`,
        { headers }
      )
    } else {
      // Load all user flashcards
      response = await axios.get(
        `${API_BASE_URL}/api/docs/flashcards`,
        { headers }
      )
    }
    
    // Filter to only unlearned cards
    const allCards = response.data.data || []
    allFlashcards.value = allCards
    flashcards.value = allCards.filter(card => !card.learned)
    
    if (flashcards.value.length === 0) {
      studyComplete.value = true
    }
  } catch (error) {
    console.error('Failed to load flashcards:', error)
    alert('Failed to load flashcards. Please try again.')
  } finally {
    loading.value = false
  }
}

function flipCard() {
  if (processingAction.value) return // Don't allow flip while processing
  isFlipped.value = !isFlipped.value
}

async function markAsLearned() {
  if (processingAction.value) return // Prevent double-click
  processingAction.value = true
  
  try {
    await updateLearnedStatus(true)
    nextCard()
  } finally {
    processingAction.value = false
  }
}

async function markAsNotLearned() {
  if (processingAction.value) return // Prevent double-click
  processingAction.value = true
  
  try {
    await updateLearnedStatus(false)
    nextCard()
  } finally {
    processingAction.value = false
  }
}

async function updateLearnedStatus(learned) {
  try {
    const headers = await auth.getAuthHeaders(router)
    await axios.put(
      `${API_BASE_URL}/api/docs/flashcards/${currentCard.value.flashcardId}/learned`,
      learned,
      { 
        headers: {
          ...headers,
          'Content-Type': 'application/json'
        }
      }
    )
    // Update the local card state as well
    if (currentCard.value) {
      currentCard.value.learned = learned
    }
  } catch (error) {
    console.error('Failed to update learned status:', error)
    console.error('Error details:', error.response?.data)
    throw error // Re-throw to be handled by caller
  }
}

function skipCard() {
  if (processingAction.value) return // Prevent skip during processing
  nextCard()
}

function nextCard() {
  isFlipped.value = false
  currentIndex.value++
  
  if (currentIndex.value >= totalCards.value) {
    studyComplete.value = true
  }
}

async function restartStudy() {
  currentIndex.value = 0
  studyComplete.value = false
  isFlipped.value = false
  await loadFlashcards()
}

async function resetAllAndRestart() {
  try {
    loading.value = true
    const headers = await auth.getAuthHeaders(router)
    
    // Reset all flashcards to unlearned
    await axios.put(
      `${API_BASE_URL}/api/docs/flashcards/reset`,
      null,
      { headers }
    )
    
    // Reload and restart
    currentIndex.value = 0
    studyComplete.value = false
    isFlipped.value = false
    await loadFlashcards()
  } catch (error) {
    console.error('Failed to reset flashcards:', error)
    alert('Failed to reset flashcards. Please try again.')
  }
}

function exitStudyMode() {
  router.push('/flashcards')
}
</script>

<style scoped>
.perspective-1000 {
  perspective: 1000px;
}

.rotate-y-180 {
  transform: rotateY(180deg);
}

.backface-hidden {
  backface-visibility: hidden;
}
</style>
