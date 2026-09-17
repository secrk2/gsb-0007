import { defineStore } from 'pinia'

let seq = 0

/** 轻量全局提示 */
export const useUiStore = defineStore('ui', {
  state: () => ({ toasts: [] }),
  actions: {
    toast(message, type = 'info') {
      const id = ++seq
      this.toasts.push({ id, message, type })
      setTimeout(() => {
        this.toasts = this.toasts.filter((t) => t.id !== id)
      }, 4200)
    },
    error(message) { this.toast(message, 'error') },
    success(message) { this.toast(message, 'success') }
  }
})
