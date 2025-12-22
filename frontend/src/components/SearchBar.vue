<template>
  <div class="flex flex-row align-middle items-center bg-surface-light-secondary text-content-primary border-2 border-border-opa rounded-full px-4 h-10 w-96 gap-3">
    <SearchIcon :size="16" class="text-content-primary" />
    <input 
      type="text" 
      :value="modelValue"
      @input="handleInput"
      @keyup.enter="$emit('search', modelValue)"
      :placeholder="placeholder"
      class="w-full h-full focus:outline-none bg-surface-light-secondary text-content-primary"
    />
    <button 
      v-if="modelValue"
      @click="clearSearch"
      class="text-content-secondary hover:text-content-primary transition-colors"
    >
      ✕
    </button>
  </div>
</template>

<script>
import SearchIcon from '@/components/icons/SearchIcon.vue'

export default {
  name: 'SearchBar',
  components: {
    SearchIcon
  },
  props: {
    modelValue: {
      type: String,
      default: ''
    },
    placeholder: {
      type: String,
      default: 'Search in Papairs'
    }
  },
  emits: ['update:modelValue', 'search'],
  methods: {
    handleInput(event) {
      const value = event.target.value
      this.$emit('update:modelValue', value)
      // Trigger search as user types (with slight debounce would be better in production)
      this.$emit('search', value)
    },
    clearSearch() {
      this.$emit('update:modelValue', '')
      this.$emit('search', '')
    }
  }
}
</script>
