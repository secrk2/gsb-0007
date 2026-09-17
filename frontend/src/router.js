import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from './stores/auth'

const routes = [
  { path: '/login', name: 'login', component: () => import('./views/LoginView.vue'), meta: { public: true } },
  { path: '/', name: 'dashboard', component: () => import('./views/DashboardView.vue'), meta: { title: '设备作战台' } },
  { path: '/devices', name: 'devices', component: () => import('./views/DevicesView.vue'), meta: { title: '设备档案' } },
  { path: '/devices/:id', name: 'device-detail', component: () => import('./views/DeviceDetailView.vue'), meta: { title: '设备详情' } },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (!to.meta.public && !auth.token) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.name === 'login' && auth.token) {
    return { path: '/' }
  }
  document.title = (to.meta.title ? to.meta.title + ' · ' : '') + '特安监管平台'
})

export default router
