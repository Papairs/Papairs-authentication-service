import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import DocsView from '../views/DocsView.vue'
import LoginView from '../views/LoginView.vue'
import RegisterView from '../views/RegisterView.vue'
import DriveView from '../views/DriveView.vue'
import SharedView from '../views/SharedView.vue'
import AutocompleteView from '../views/AutocompleteView.vue'
import FlashcardsView from '../views/FlashcardsView.vue'
import StudyModeView from '../views/StudyModeView.vue'
import AIView from '../views/AIView.vue'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: HomeView
  },
  {
    path: '/drive',
    name: 'Drive',
    component: DriveView
  },
  {
    path: '/drive/shared',
    name: 'SharedWithMe',
    component: SharedView
  },
  {
    path: '/drive/:folderId',
    name: 'DriveFolder',
    component: DriveView,
    props: true
  },
  {
    path: '/docs',
    name: 'Docs',
    component: DocsView
  },
  {
    path: '/docs/:id',
    name: 'DocsWithId',
    component: DocsView,
    props: true
  },
  {
    path: '/login',
    name: 'Login',
    component: LoginView
  },
  {
    path: '/autocomplete',
    name: 'Autocomplete',
    component: AutocompleteView
  },
  {
    path: '/register',
    name: 'Register',
    component: RegisterView
  },
  {
    path: '/flashcards',
    name: 'Flashcards',
    component: FlashcardsView
  },
  {
    path: '/study',
    name: 'StudyMode',
    component: StudyModeView
  },
  {
    path: '/ai',
    name: 'AI',
    component: AIView
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router