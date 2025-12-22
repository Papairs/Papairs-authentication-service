<script>
import axios from 'axios'
import preview from '../../Images/7 Semester Recording Oct 13 2025.gif'
import LoginHeader from '../components/LoginHeader.vue'
import RegisterForm from '../components/RegisterForm.vue'
import ImagePanel from '../components/ImagePanel.vue'
import { API_BASE_URL } from '@/config'

export default {
  name: 'RegisterView',
  components: {
    LoginHeader,
    RegisterForm,
    ImagePanel
  },
  data() {
    return {
      // subtle orange grid using theme accent color
      gridBg: {
        backgroundImage:
          'linear-gradient(to right, rgba(255,119,0,0.08) 2px, transparent 2px),' +
          'linear-gradient(to bottom, rgba(255,119,0,0.08) 2px, transparent 2px)',
        backgroundSize: '80px 80px, 80px 80px',
        backgroundPosition: '0 0, 0 0'
      },
      previewImg: preview
    }
  },
  methods: {
    async handleRegister(registerData) {
      try {
        const resp = await axios.post(`${API_BASE_URL}/api/auth/register`, {
          email: registerData.email,
          password: registerData.password
        })
        
        if (resp.status === 201 || (resp.status === 200 && resp.data)) {
          this.$refs.registerForm.error = null
          this.$refs.registerForm.success = 'Account created successfully! You can now login.'
          this.$refs.registerForm.loading = false
          // Redirect to login after 2 seconds
          setTimeout(() => {
            this.$router.push({ name: 'Login' })
          }, 2000)
        } else {
          this.$refs.registerForm.error = 'Registration failed. Please try again.'
          this.$refs.registerForm.loading = false
        }
      } catch (err) {
        console.error('Registration error:', err)
        
        // Handle specific error codes
        if (err.response?.status === 409) {
          this.$refs.registerForm.error = 'This email is already registered. Please use a different email or try logging in.'
        } else if (err.response?.data?.message) {
          this.$refs.registerForm.error = err.response.data.message
        } else if (err.message) {
          this.$refs.registerForm.error = err.message
        } else {
          this.$refs.registerForm.error = 'Registration failed. Please try again.'
        }
        this.$refs.registerForm.loading = false
      }
    },
    
    handleBackToLogin() {
      this.$router.push({ name: 'Login' })
    }
  }
}
</script>

<style scoped>
/* keep checkbox looking neat in all browsers */
input[type="checkbox"] {
  accent-color: #FF7700;
}
</style>

<template>
  <div class="min-h-screen bg-surface-light" :style="gridBg">
    <LoginHeader />
    
    <!-- Main content area with specified padding -->
    <div class="px-32 py-16">
      <div class="flex min-h-[calc(100vh-364px)]">
        <RegisterForm 
          ref="registerForm"
          @register="handleRegister"
          @back-to-login="handleBackToLogin"
        />
        
        <ImagePanel 
          :image-src="previewImg"
          image-alt="Papairs preview"
        />
      </div>
    </div>
  </div>
</template>

