import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import DocsView from '../views/DocsView.vue'
import LoginView from '../views/LoginView.vue'
import DriveView from '../views/DriveView.vue'

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
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router