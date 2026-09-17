import { defineStore } from 'pinia'
import { api } from '../api/client'

const QUEUE_KEY = 'tean_offline_queue'
const MANUAL_KEY = 'tean_manual_offline'
const CACHE_PREFIX = 'tean_cache:'

/**
 * 离线中心：
 * - online = 浏览器在线 且 未手动断网（顶栏可切换“模拟断网”演示外巡检场景）
 * - 离线时 GET 走本地缓存快照并明确标注，绝不把旧状态当最新数据静默展示
 * - 离线提交的维保记录进入队列（携带幂等键 recordNo），恢复后批量同步
 */
export const useOfflineStore = defineStore('offline', {
  state: () => ({
    browserOnline: navigator.onLine,
    manualOffline: localStorage.getItem(MANUAL_KEY) === '1',
    queue: JSON.parse(localStorage.getItem(QUEUE_KEY) || '[]'),
    syncing: false,
    lastSyncText: ''
  }),
  getters: {
    online: (s) => s.browserOnline && !s.manualOffline,
    pendingCount: (s) => s.queue.length
  },
  actions: {
    init() {
      window.addEventListener('online', () => {
        this.browserOnline = true
        this.flush()
      })
      window.addEventListener('offline', () => {
        this.browserOnline = false
      })
    },
    toggleManual() {
      this.manualOffline = !this.manualOffline
      localStorage.setItem(MANUAL_KEY, this.manualOffline ? '1' : '0')
      if (!this.manualOffline) this.flush()
    },
    persist() {
      localStorage.setItem(QUEUE_KEY, JSON.stringify(this.queue))
    },
    /** 离线维保记录入队；recordNo 在入队时生成，重传幂等 */
    enqueueMaintenance({ deviceId, content }) {
      const item = {
        recordNo: crypto.randomUUID(),
        deviceId,
        content,
        clientCreatedAt: new Date().toISOString().slice(0, 19),
        offline: true
      }
      this.queue.push(item)
      this.persist()
      return item
    },
    async flush() {
      if (!this.online || this.syncing || this.queue.length === 0) return
      this.syncing = true
      try {
        const { data } = await api.post('/maintenance/sync', { items: this.queue })
        const result = data.data
        this.queue = []
        this.persist()
        this.lastSyncText = `已同步 ${result.created} 条，幂等去重 ${result.duplicated} 条`
      } catch (e) {
        this.lastSyncText = '同步失败，将在下次联网时重试'
      } finally {
        this.syncing = false
      }
    },
    writeCache(key, data) {
      try {
        localStorage.setItem(CACHE_PREFIX + key, JSON.stringify({ data, at: Date.now() }))
      } catch (e) { /* 存储满时忽略 */ }
    },
    readCache(key) {
      try {
        const raw = localStorage.getItem(CACHE_PREFIX + key)
        return raw ? JSON.parse(raw) : null
      } catch (e) {
        return null
      }
    }
  }
})
