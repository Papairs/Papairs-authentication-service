<script>
export default {
  name: 'AuthWarning',
  props: {
    message: {
      type: String,
      default: ''
    }
  }
}
</script>

<template>
  <div class="bg-yellow-100 border border-yellow-300 text-yellow-800 px-4 py-3 rounded mb-6">
    <div class="flex">
      <div class="flex-shrink-0">
        <span class="text-lg">⚠️</span>
      </div>
      <div class="ml-3">
        <p class="text-sm font-medium">
          Authentication Required
        </p>
        <p class="text-sm">
          {{ message || 'These operations require authentication. Please login first using the Auth section above.' }}
        </p>
      </div>
    </div>
  </div>
</template>
