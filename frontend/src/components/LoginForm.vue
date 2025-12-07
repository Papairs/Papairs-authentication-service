<script>
export default {
  name: 'LoginForm',
  emits: ['login', 'create-account'],
  data() {
    return {
      formData: {
        email: '',
        password: '',
        remember: false
      },
      loading: false,
      error: null
    }
  },
  methods: {
    handleSubmit() {
      this.error = null
      this.loading = true
      
      this.$emit('login', {
        email: this.formData.email,
        password: this.formData.password,
        remember: this.formData.remember
      })
    }
  }
}
</script>

<style scoped>
input[type="checkbox"] {
  accent-color: #FF7700; /* Use theme accent color */
}
</style>

<template>
  <div class="w-[500px] p-2.5 flex-shrink-0 border-content-primary text-content-primary">
    <div class="pt-12">
      <h1 class="text-6xl lg:text-7xl font-extrabold mb-6">
        Sign In<span class="text-accent">.</span>
      </h1>

      <form @submit.prevent="handleSubmit" class="max-w-sm">
        <div class="mb-10">
          <input 
            placeholder="Email*"
            v-model="formData.email" 
            type="email" 
            required
            class="w-full bg-transparent border-b-2 border-content-primary py-2 px-1 text-xl outline-none placeholder-content-primary"
          />
        </div>

        <div class="mb-6">
          <input 
            placeholder="Password*"
            v-model="formData.password" 
            type="password" 
            required
            class="w-full bg-transparent border-b-2 border-content-primary py-2 px-1 text-xl s outline-none placeholder-content-primary"
          />
        </div>

        <div class="flex items-center mt-6 mb-6">
          <label class="inline-flex items-center">
            <input 
              type="checkbox" 
              v-model="formData.remember" 
              class="h-5 w-5 rounded-sm border border-content-primary"
            />
            <span class="ml-3 text-content-primary">Remember me</span>
          </label>
        </div>

        <div class="mb-6">
          <a href="#" class="text-sm text-content-primary font-medium hover:underline">Password Help?</a>
        </div>

        <div class="mb-8">
          <button 
            :disabled="loading" 
            type="submit"
            class=" bg-content-primary text-content-inverse rounded-md py-3 px-6 w-40 text-center font-semibold hover:opacity-90 disabled:opacity-60"
          >
            <span v-if="!loading">Sign In</span>
            <span v-else>Signing in...</span>
          </button>
        </div>

        <div class="text-sm text-content-primary">
          Don't have an account? <br />
          <a @click.prevent="$emit('create-account')" class="font-semibold underline cursor-pointer">
            Create One Now
          </a>
        </div>

        <p v-if="error" class="mt-6 text-sm text-red">{{ error }}</p>
      </form>
    </div>
  </div>
</template>

