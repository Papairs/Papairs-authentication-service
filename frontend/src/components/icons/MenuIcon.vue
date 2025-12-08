<template>
  <svg 
    :width="size" 
    :height="size" 
    viewBox="0 0 24 24"
    :class="className"
    fill="none"
  >
    <path 
      stroke-linecap="round" 
      stroke-linejoin="round" 
      stroke-width="2" 
      d="M4 6h16M4 12h16M4 18h16"
      stroke="currentColor"
    />
  </svg>
</template>

<script>
export default {
  name: 'MenuIcon',
  props: {
    size: {
      type: Number,
      default: 20
    },
    className: {
      type: String,
      default: ''
    }
  }
}
</script>
