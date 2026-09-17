import { defineStore } from 'pinia'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('tean.token') || '',
    user: JSON.parse(localStorage.getItem('tean.user') || 'null')
  }),
  getters: {
    isLoggedIn: (s) => !!s.token,
    roleLabel: (s) => ({
      DEVICE_ADMIN: '设备管理员',
      MAINTAINER: '维保人员',
      INSPECTOR: '检验员',
      SUPERVISOR: '监察员'
    }[s.user?.role] || '')
  },
  actions: {
    login(token, user) {
      this.token = token
      this.user = user
      localStorage.setItem('tean.token', token)
      localStorage.setItem('tean.user', JSON.stringify(user))
    },
    logout() {
      this.token = ''
      this.user = null
      localStorage.removeItem('tean.token')
      localStorage.removeItem('tean.user')
    }
  }
})
