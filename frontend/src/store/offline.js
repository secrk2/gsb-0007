import { defineStore } from 'pinia'
import api from '../api'

const QUEUE_KEY = 'tean.queue'

/**
 * 离线管理：
 * - 跟踪网络状态；断网时维保记录进入本地队列（localStorage 持久化）
 * - 恢复在线后自动合并到服务端；服务端按 clientId 幂等去重，不产生重复记录
 */
export const useOfflineStore = defineStore('offline', {
  state: () => ({
    online: navigator.onLine,
    queue: JSON.parse(localStorage.getItem(QUEUE_KEY) || '[]'),
    syncing: false,
    lastSync: null // { created, duplicate, at }
  }),
  actions: {
    init() {
      window.addEventListener('online', () => {
        this.online = true
        this.flush()
      })
      window.addEventListener('offline', () => {
        this.online = false
      })
      if (this.online && this.queue.length) {
        this.flush()
      }
    },
    enqueue(record) {
      this.queue.push(record)
      this.persist()
    },
    persist() {
      localStorage.setItem(QUEUE_KEY, JSON.stringify(this.queue))
    },
    async flush() {
      if (this.syncing || !this.queue.length) return
      this.syncing = true
      try {
        const results = await api.post('/maintenance/sync', this.queue)
        const created = results.filter((r) => r.status === 'CREATED').length
        const duplicate = results.filter((r) => r.status === 'DUPLICATE').length
        this.lastSync = { created, duplicate, at: Date.now() }
        this.queue = []
        this.persist()
      } catch (e) {
        // 同步失败保留队列，下次在线再试
      } finally {
        this.syncing = false
      }
    }
  }
})
