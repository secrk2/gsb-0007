import axios from 'axios'
import { useAuthStore } from '../stores/auth'
import { useOfflineStore } from '../stores/offline'
import router from '../router'

export const api = axios.create({ baseURL: '/api', timeout: 15000 })

api.interceptors.request.use((config) => {
  const auth = useAuthStore()
  if (auth.token) {
    config.headers.Authorization = `Bearer ${auth.token}`
  }
  return config
})

api.interceptors.response.use(
  (resp) => resp,
  (error) => {
    const status = error.response?.status || 0
    const message = error.response?.data?.message
      || (error.code === 'ECONNABORTED' ? '请求超时，请重试' : '网络异常，请检查连接')
    if (status === 401) {
      const auth = useAuthStore()
      auth.logout()
      if (router.currentRoute.value.name !== 'login') {
        router.push({ name: 'login' })
      }
    }
    return Promise.reject({ status, message, raw: error })
  }
)

/**
 * 带离线缓存的 GET。
 * 在线：请求并写入缓存；离线：返回缓存快照并标记 offline/cachedAt，
 * 由页面以横幅明示“数据可能已过期”，绝不静默展示旧状态。
 */
export async function apiGet(url, config = {}) {
  const offline = useOfflineStore()
  if (!offline.online) {
    const hit = offline.readCache(url)
    if (hit) {
      return { data: hit.data, offline: true, cachedAt: hit.at }
    }
    throw { status: 0, message: '当前处于离线状态，且本机没有该数据的缓存', offlineNoCache: true }
  }
  const resp = await api.get(url, config)
  offline.writeCache(url, resp.data)
  return { data: resp.data, offline: false }
}
