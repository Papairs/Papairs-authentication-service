<script>
import axios from 'axios'
import preview from '../../Images/7 Semester Recording Oct 13 2025.gif'
import LoginHeader from '../components/LoginHeader.vue'
import RegisterForm from '../components/RegisterForm.vue'
import ImagePanel from '../components/ImagePanel.vue'

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
        const resp = await axios.post('http://localhost:8081/api/auth/register', {
          email: registerData.email,
          password: registerData.password
        })
        
        if (resp.data?.success) {
          this.$refs.registerForm.success = 'Account created successfully! You can now login.'
          // Redirect to login after 2 seconds
          setTimeout(() => {
            this.$router.push({ name: 'Login' })
          }, 2000)
        } else {
          this.$refs.registerForm.error = 'Registration failed. Please try again.'
        }
      } catch (err) {
        this.$refs.registerForm.error = err.response?.data?.message || err.message || 'Registration failed'
      } finally {
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
  <div class="min-h-screen bg-surface-light dark:bg-surface-dark" :style="gridBg">
    <!-- Header with height 100px -->
    <LoginHeader />
    
    <!-- Main content area with specified padding -->
    <div class="px-32 py-16">
      <div class="flex min-h-[calc(100vh-364px)]">
        <!-- Register Form Component - width 500px + 10px padding -->
        <RegisterForm 
          ref="registerForm"
          @register="handleRegister"
          @back-to-login="handleBackToLogin"
        />
        
        <!-- Image Panel Component - fills remaining space with 16:9 ratio -->
        <ImagePanel 
          :image-src="previewImg"
          image-alt="Papairs preview"
        />
      </div>
    </div>
  </div>
</template>

