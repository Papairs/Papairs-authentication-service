<template>
  <svg 
    :width="size" 
    :height="size" 
    viewBox="0 0 24 24"
    :class="className"
  >
    <path 
      d="M6 9l6 6 6-6" 
      fill="none" 
      stroke="currentColor" 
      stroke-linecap="round" 
      stroke-linejoin="round" 
      stroke-width="2"
    />
  </svg>
</template>

<script>
export default {
  name: 'ChevronDownIcon',
  props: {
    size: {
      type: Number,
      default: 16
    },
    className: {
      type: String,
      default: ''
    }
  }
}
</script>
