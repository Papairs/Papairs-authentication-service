import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import DocsViewYjs from '../views/DocsViewYjs.vue'
import LoginView from '../views/LoginView.vue'
import RegisterView from '../views/RegisterView.vue'
import DriveView from '../views/DriveView.vue'
import AutocompleteView from '../views/AutocompleteView.vue'
import FlashcardsView from '../views/FlashcardsView.vue'
import StudyModeView from '../views/StudyModeView.vue'
import AIView from '../views/AIView.vue'
import auth from '../utils/auth'

const routes = [
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/debug',
    name: 'Home',
    component: HomeView,
    meta: { requiresAuth: true }
  },
  {
    path: '/drive',
    name: 'Drive',
    component: DriveView,
    meta: { requiresAuth: true }
  },
  {
    path: '/drive/shared',
    name: 'SharedWithMe',
    component: DriveView,
    meta: { requiresAuth: true }
  },
  {
    path: '/drive/:folderId',
    name: 'DriveFolder',
    component: DriveView,
    props: true,
    meta: { requiresAuth: true }
  },
  {
    path: '/docs',
    name: 'Docs',
    component: DocsViewYjs,
    meta: { requiresAuth: true }
  },
  {
    path: '/docs/:id',
    name: 'DocsWithId',
    component: DocsViewYjs,
    props: true,
    meta: { requiresAuth: true }
  },
  {
    path: '/login',
    name: 'Login',
    component: LoginView
  },
  {
    path: '/autocomplete',
    name: 'Autocomplete',
    component: AutocompleteView,
    meta: { requiresAuth: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: RegisterView
  },
  {
    path: '/flashcards',
    name: 'Flashcards',
    component: FlashcardsView,
    meta: { requiresAuth: true }
  },
  {
    path: '/study',
    name: 'StudyMode',
    component: StudyModeView,
    meta: { requiresAuth: true }
  },
  {
    path: '/ai',
    name: 'AI',
    component: AIView,
    meta: { requiresAuth: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const requiresAuth = to.matched.some(record => record.meta.requiresAuth)
  const isAuthenticated = auth.isAuthenticated()

  if (requiresAuth && !isAuthenticated) {
    next('/login')
  } else if ((to.path === '/login' || to.path === '/register') && isAuthenticated) {
    next('/drive')
  } else {
    next()
  }
})

export default router