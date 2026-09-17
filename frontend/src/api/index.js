import axios from 'axios'
import { useAuthStore } from '../store/auth'
import router from '../router'

const api = axios.create({ baseURL: '/api', timeout: 12000 })

api.interceptors.request.use((config) => {
  const auth = useAuthStore()
  if (auth.token) {
    config.headers.Authorization = `Bearer ${auth.token}`
  }
  return config
})

function normalizeError(err) {
  if (err.response) {
    const body = err.response.data
    const e = new Error(body?.message || '请求失败')
    e.code = body?.code ?? err.response.status
    return e
  }
  const e = new Error('网络不可用，请检查连接')
  e.code = -1
  e.network = true
  return e
}

api.interceptors.response.use(
  (resp) => {
    const body = resp.data
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code === 0) return body.data
      const err = new Error(body.message || '请求失败')
      err.code = body.code
      if (body.code === 401) {
        useAuthStore().logout()
        router.push('/login')
      }
      throw err
    }
    return body
  },
  (err) => {
    const e = normalizeError(err)
    if (e.code === 401) {
      useAuthStore().logout()
      router.push('/login')
    }
    throw e
  }
)

/**
 * 带本地快照的 GET：在线时刷新缓存；断网时回退到缓存并标注时间，
 * 让页面明确展示「这是历史数据」，而不是拿旧状态冒充最新。
 */
export async function cachedGet(url, config) {
  const key = 'tean.cache:' + url + JSON.stringify(config?.params || {})
  try {
    const data = await api.get(url, config)
    localStorage.setItem(key, JSON.stringify({ at: Date.now(), data }))
    return { data, cached: false, cachedAt: null }
  } catch (e) {
    if (e.network) {
      const raw = localStorage.getItem(key)
      if (raw) {
        const { at, data } = JSON.parse(raw)
        return { data, cached: true, cachedAt: at }
      }
    }
    throw e
  }
}

export default api
