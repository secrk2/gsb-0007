import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../store/auth'

const routes = [
  { path: '/login', component: () => import('../views/LoginView.vue'), meta: { public: true } },
  {
    path: '/',
    component: () => import('../components/AppShell.vue'),
    children: [
      { path: '', redirect: '/dashboard' },
      { path: 'dashboard', component: () => import('../views/DashboardView.vue'), meta: { title: '设备作战台' } },
      { path: 'devices', component: () => import('../views/DevicesView.vue'), meta: { title: '设备档案' } },
      { path: 'devices/:id', component: () => import('../views/DeviceDetailView.vue'), meta: { title: '设备详情' } },
      { path: 'maintenance', component: () => import('../views/MaintenanceView.vue'), meta: { title: '维保巡检' } },
      { path: '403', component: () => import('../views/ForbiddenView.vue'), meta: { title: '无权访问' } }
    ]
  },
  { path: '/:pathMatch(.*)*', component: () => import('../views/NotFoundView.vue'), meta: { public: true } }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (!to.meta.public && !auth.isLoggedIn) {
    return '/login'
  }
  if (to.path === '/login' && auth.isLoggedIn) {
    return '/'
  }
})

export default router
