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
      d="M20.354 15.354A9 9 0 018.646 3.646 9.003 9.003 0 0012 21a9.003 9.003 0 008.354-5.646z"
      stroke="currentColor"
    />
  </svg>
</template>

<script>
export default {
  name: 'DarkModeIcon',
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
