<script>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { API_BASE_URL } from '@/config'
import auth from '@/utils/auth'
import axios from 'axios'
import FlashcardsHeader from '@/components/FlashcardsHeader.vue'
import FlashcardsGrid from '@/components/FlashcardsGrid.vue'

export default {
  name: 'FlashcardsView',
  components: {
    FlashcardsHeader,
    FlashcardsGrid
  },
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
        const response = await axios.get(`${API_BASE_URL}/api/docs/flashcards`, {
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
            title: card.pageId,
            count: 0
          })
        }
        pageMap.get(card.pageId).count++
      })
      
      // Fetch page details for all unique pages
      await loadPageDetails(pageMap)
      
      pages.value = Array.from(pageMap.values())
    }

    async function loadPageDetails(pageMap) {
      try {
        const headers = await auth.getAuthHeaders(router)
        
        for (const [pageId, pageData] of pageMap.entries()) {
          try {
            const response = await axios.get(
              `${API_BASE_URL}/api/docs/pages/${pageId}`,
              { headers }
            )
            
            if (response.data) {
              pageData.title = response.data.title || pageId
              pageDetails.value.set(pageId, {
                title: response.data.title || pageId,
                pageId: pageId
              })
            }
          } catch (error) {
            // If we can't fetch page details, use pageId as fallback
            pageData.title = pageId
            pageDetails.value.set(pageId, {
              title: pageId,
              pageId: pageId
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

    const filteredFlashcards = computed(() => {
      if (selectedPageId.value === 'all') {
        return flashcards.value
      }
      return flashcards.value.filter(card => card.pageId === selectedPageId.value)
    })

    async function deleteFlashcard(flashcardId) {
      if (!confirm('Are you sure you want to delete this flashcard?')) {
        return
      }

      try {
        const headers = await auth.getAuthHeaders(router)
        await axios.delete(`${API_BASE_URL}/api/docs/flashcards/${flashcardId}`, {
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
          `${API_BASE_URL}/api/docs/flashcards/reset`,
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
      flippedCards,
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
    <FlashcardsHeader
      :loading="loading"
      :flashcards-count="flashcards.length"
      :selected-page-id="selectedPageId"
      :pages="pages"
      @update:selected-page-id="selectedPageId = $event"
      @start-study="startStudyMode"
      @reset-all="resetAllFlashcards"
    />

    <!-- Content -->
    <FlashcardsGrid
      :flashcards="flashcards"
      :loading="loading"
      :flipped-cards="flippedCards"
      :filtered-flashcards="filteredFlashcards"
      :get-page-title="getPageTitle"
      @toggle-flip="toggleFlip"
      @delete="deleteFlashcard"
    />
  </div>
</template>
