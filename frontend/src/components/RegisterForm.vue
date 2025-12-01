<script>
export default {
  name: 'RegisterForm',
  emits: ['register', 'back-to-login'],
  data() {
    return {
      formData: {
        email: '',
        password: '',
        confirmPassword: ''
      },
      loading: false,
      error: null,
      success: null
    }
  },
  computed: {
    hasLowercase() {
      return /[a-z]/.test(this.formData.password)
    },
    hasUppercase() {
      return /[A-Z]/.test(this.formData.password)
    },
    hasNumber() {
      return /\d/.test(this.formData.password)
    },
    hasSpecial() {
      return /[@$!%*?&]/.test(this.formData.password)
    },
    hasMinLength() {
      return this.formData.password.length >= 8
    },
    passwordsMatch() {
      return this.formData.password === this.formData.confirmPassword
    },
    isPasswordValid() {
      return this.hasLowercase && this.hasUppercase && this.hasNumber && this.hasSpecial && this.hasMinLength
    },
    isFormValid() {
      return this.formData.email && 
             this.isPasswordValid && 
             this.passwordsMatch && 
             this.formData.confirmPassword
    }
  },
  methods: {
    handleSubmit() {
      this.error = null
      this.success = null
      
      if (!this.passwordsMatch) {
        this.error = 'Passwords do not match'
        return
      }
      
      if (!this.isPasswordValid) {
        this.error = 'Password does not meet requirements'
        return
      }
      
      this.loading = true
      
      this.$emit('register', {
        email: this.formData.email,
        password: this.formData.password
      })
    }
  }
}
</script>

<style scoped>
input[type="checkbox"] {
  accent-color: #FF7700;
}
</style>

<template>
  <div class="w-[500px] p-2.5 flex-shrink-0">
    <div class="pt-12">
      <h1 class="text-6xl lg:text-7xl font-extrabold text-content-primary mb-6">
        Register<span class="text-accent">.</span>
      </h1>

      <form @submit.prevent="handleSubmit" class="max-w-sm">
        <div class="mb-10">
          <input 
            placeholder="Email*"
            v-model="formData.email" 
            type="email" 
            required
            class="w-full bg-transparent border-0 border-b-2 border-border-light-subtle focus:border-content-primary py-2 px-1 text-xl outline-none text-content-primary placeholder-content-secondary"
          />
        </div>

        <div class="mb-6">
          <input 
            placeholder="Password*"
            v-model="formData.password" 
            type="password" 
            required
            class="w-full bg-transparent border-0 border-b-2 border-border-light-subtle focus:border-content-primary py-2 px-1 text-xl outline-none text-content-primary placeholder-content-secondary"
          />
        </div>

        <div class="mb-8">
          <input 
            placeholder="Confirm Password*"
            v-model="formData.confirmPassword" 
            type="password" 
            required
            class="w-full bg-transparent border-0 border-b-2 border-border-light-subtle focus:border-content-primary py-2 px-1 text-xl outline-none text-content-primary placeholder-content-secondary"
          />
        </div>

        <!-- Password Requirements -->
        <div class="mb-6 text-xs text-content-secondary">
          <p class="mb-1">Password must contain:</p>
          <ul class="list-disc ml-4 space-y-1">
            <li :class="{'text-green-600': hasLowercase, 'text-content-secondary': !hasLowercase}">
              One lowercase letter
            </li>
            <li :class="{'text-green-600': hasUppercase, 'text-content-secondary': !hasUppercase}">
              One uppercase letter
            </li>
            <li :class="{'text-green-600': hasNumber, 'text-content-secondary': !hasNumber}">
              One number
            </li>
            <li :class="{'text-green-600': hasSpecial, 'text-content-secondary': !hasSpecial}">
              One special character (@$!%*?&)
            </li>
            <li :class="{'text-green-600': hasMinLength, 'text-content-secondary': !hasMinLength}">
              At least 8 characters
            </li>
          </ul>
        </div>

        <div class="mb-8">
          <button 
            :disabled="loading || !isFormValid" 
            type="submit"
            class="bg-content-black text-content-inverse rounded-md py-3 px-6 w-40 text-center font-semibold hover:opacity-90 disabled:opacity-60"
          >
            <span v-if="!loading">Register</span>
            <span v-else>Registering...</span>
          </button>
        </div>

        <div class="text-sm text-content-secondary">
          Already have an account? <br />
          <a @click.prevent="$emit('back-to-login')" class="font-semibold underline cursor-pointer text-content-primary">
            Sign In Here
          </a>
        </div>

        <p v-if="error" class="mt-6 text-sm text-red">{{ error }}</p>
        <p v-if="success" class="mt-6 text-sm text-green-600">{{ success }}</p>
      </form>
    </div>
  </div>
</template>
