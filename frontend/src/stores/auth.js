import { defineStore } from 'pinia'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('tean_token') || '',
    user: JSON.parse(localStorage.getItem('tean_user') || 'null')
  }),
  getters: {
    isLoggedIn: (s) => !!s.token,
    isSupervisor: (s) => s.user?.role === 'SUPERVISOR',
    canTransition: (s) => ['DEVICE_ADMIN', 'INSPECTOR'].includes(s.user?.role),
    canResolveHazard: (s) => ['DEVICE_ADMIN', 'INSPECTOR', 'SUPERVISOR'].includes(s.user?.role)
  },
  actions: {
    setSession(token, user) {
      this.token = token
      this.user = user
      localStorage.setItem('tean_token', token)
      localStorage.setItem('tean_user', JSON.stringify(user))
    },
    logout() {
      this.token = ''
      this.user = null
      localStorage.removeItem('tean_token')
      localStorage.removeItem('tean_user')
    }
  }
})
