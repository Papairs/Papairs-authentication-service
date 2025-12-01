<script>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import auth from '@/utils/auth'
import axios from 'axios'

export default {
  name: 'FlashcardsView',
  setup() {
    const router = useRouter()
    const flashcards = ref([])
    const loading = ref(false)
    const flippedCards = ref(new Set())
    const selectedPageId = ref('all')
    const pages = ref([])
    const pageDetails = ref(new Map())

    async function loadFlashcards() {
      loading.value = true
      try {
        const headers = await auth.getAuthHeaders(router)
        const response = await axios.get('http://localhost:8082/api/docs/flashcards', {
          headers
        })
        
        if (response.data.success) {
          flashcards.value = response.data.data || []
          await extractUniquePages()
        }
      } catch (error) {
        console.error('Failed to load flashcards:', error)
        flashcards.value = []
      } finally {
        loading.value = false
      }
    }

    async function extractUniquePages() {
      const pageMap = new Map()
      flashcards.value.forEach(card => {
        if (!pageMap.has(card.pageId)) {
          pageMap.set(card.pageId, {
            pageId: card.pageId,
            count: 0
          })
        }
        pageMap.get(card.pageId).count++
      })
      pages.value = Array.from(pageMap.values())
      
      // Fetch page details for all unique pages
      await loadPageDetails()
    }

    async function loadPageDetails() {
      try {
        const headers = await auth.getAuthHeaders(router)
        
        for (const page of pages.value) {
          try {
            const response = await axios.get(
              `http://localhost:8082/api/docs/pages/${page.pageId}`,
              { headers }
            )
            
            if (response.data) {
              pageDetails.value.set(page.pageId, {
                title: response.data.title || page.pageId,
                pageId: page.pageId
              })
            }
          } catch (error) {
            // If we can't fetch page details, use pageId as fallback
            pageDetails.value.set(page.pageId, {
              title: page.pageId,
              pageId: page.pageId
            })
          }
        }
      } catch (error) {
        console.error('Failed to load page details:', error)
      }
    }

    function getPageTitle(pageId) {
      const details = pageDetails.value.get(pageId)
      return details ? details.title : pageId
    }

    function filteredFlashcards() {
      if (selectedPageId.value === 'all') {
        return flashcards.value
      }
      return flashcards.value.filter(card => card.pageId === selectedPageId.value)
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

    function startStudyMode() {
      // Navigate to study mode with current filter
      router.push({
        path: '/study',
        query: selectedPageId.value !== 'all' ? { pageId: selectedPageId.value } : {}
      })
    }

    async function resetAllFlashcards() {
      if (!confirm('Are you sure you want to reset ALL flashcards to unlearned? This will allow you to study them all again.')) {
        return
      }

      try {
        const headers = await auth.getAuthHeaders(router)
        await axios.put(
          'http://localhost:8082/api/docs/flashcards/reset',
          null,
          { headers }
        )
        
        alert('All flashcards have been reset to unlearned!')
        await loadFlashcards()
      } catch (error) {
        console.error('Failed to reset flashcards:', error)
        alert('Failed to reset flashcards. Please try again.')
      }
    }

    onMounted(() => {
      loadFlashcards()
    })

    return {
      flashcards,
      loading,
      deleteFlashcard,
      toggleFlip,
      isFlipped,
      selectedPageId,
      pages,
      filteredFlashcards,
      getPageTitle,
      startStudyMode,
      resetAllFlashcards
    }
  }
}
</script>

<template>
  <div class="flex flex-col h-full w-full overflow-hidden bg-surface-light dark:bg-surface-dark">
    <!-- Header -->
    <div class="flex flex-row h-[50px] w-full border-b-2 border-accent flex-shrink-0 items-center px-6 justify-between">
        <h1 class="text-2xl font-bold text-content-primary dark:text-content-inverse">My Flashcards</h1>
        
        <!-- Filter by Document and Study Button -->
        <div v-if="!loading && flashcards.length > 0" class="flex items-center gap-3">
          <label for="page-filter" class="text-sm text-content-secondary font-medium">
            Filter by Document:
          </label>
          <select
            id="page-filter"
            v-model="selectedPageId"
            class="px-4 py-2 border border-border-light-subtle dark:border-border-dark-subtle rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-accent bg-white dark:bg-surface-dark-secondary text-content-primary dark:text-content-inverse"
          >
            <option value="all">All Documents ({{ flashcards.length }})</option>
            <option
              v-for="page in pages"
              :key="page.pageId"
              :value="page.pageId"
            >
              {{ getPageTitle(page.pageId) }} ({{ page.count }})
            </option>
          </select>
          
          <button
            @click="startStudyMode"
            class="px-4 py-2 bg-accent text-white rounded-md text-sm font-medium hover:bg-accent-dark transition-colors flex items-center gap-2"
          >
            <span>📚</span>
            Study Mode
          </button>
          
          <button
            @click="resetAllFlashcards"
            class="px-4 py-2 bg-green-600 text-white rounded-md text-sm font-medium hover:bg-green-700 transition-colors flex items-center gap-2"
            title="Reset all flashcards to unlearned"
          >
            <span>🔄</span>
            Reset All
          </button>
        </div>
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

        <div v-else-if="filteredFlashcards().length === 0" class="text-center py-8">
          <p class="text-content-secondary text-lg mb-4">No flashcards for this document</p>
          <p class="text-content-secondary">Try selecting a different document or view all flashcards.</p>
        </div>
        
        <!-- Flashcards Grid -->
        <div v-else class="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
          <div
            v-for="card in filteredFlashcards()"
            :key="card.flashcardId"
            class="bg-white dark:bg-surface-dark-secondary border border-border-light-subtle dark:border-border-dark-subtle rounded-lg p-6 hover:shadow-lg transition-shadow cursor-pointer"
            @click="toggleFlip(card.flashcardId)"
          >
            <!-- Card Header -->
            <div class="flex justify-between items-start mb-4">
              <span class="text-xs px-2 py-1 bg-surface-light dark:bg-surface-dark text-content-secondary rounded border border-border-light-subtle dark:border-border-dark-subtle truncate max-w-[200px]" :title="getPageTitle(card.pageId)">
                📄 {{ getPageTitle(card.pageId) }}
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
                <p class="text-content-primary dark:text-content-inverse font-medium">{{ card.question }}</p>
              </div>
              
              <div v-else>
                <h3 class="text-sm text-content-secondary mb-2">Answer:</h3>
                <p class="text-content-primary dark:text-content-inverse">{{ card.answer }}</p>
              </div>
            </div>

            <!-- Card Footer -->
            <div class="mt-4 pt-4 border-t border-border-light-subtle dark:border-border-dark-subtle text-center">
              <span class="text-sm text-content-secondary">
                {{ isFlipped(card.flashcardId) ? 'Click to see question' : 'Click to see answer' }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
</template>
