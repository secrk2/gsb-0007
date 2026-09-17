<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1>设备档案</h1>
        <p class="page-sub">共 {{ page.totalElements }} 台 · 位置信息已脱敏为「区域 · 编号」</p>
      </div>
    </div>

    <div v-if="snapshot" class="snapshot-tip">⚠ 离线快照（{{ snapshotAt }}），数据可能已过期</div>

    <div class="filter-bar">
      <input v-model.trim="filters.keyword" placeholder="搜索名称 / 注册代码 / 编号" @keyup.enter="search" />
      <select v-model="filters.status" @change="search">
        <option value="">全部状态</option>
        <option v-for="s in meta.statuses" :key="s.value" :value="s.value">{{ s.label }}</option>
      </select>
      <select v-model="filters.type" @change="search">
        <option value="">全部类型</option>
        <option v-for="t in meta.types" :key="t.value" :value="t.value">{{ t.label }}</option>
      </select>
      <button class="btn" @click="search">查询</button>
    </div>

    <ErrorState v-if="error" :code="errorStatus" title="列表加载失败" :message="error" />
    <p v-else-if="!loading && !page.content.length" class="empty">没有符合条件的设备</p>

    <div v-else class="device-list">
      <div class="device-row device-row-head">
        <span>设备</span><span>类型</span><span>状态</span><span>位置（已脱敏）</span><span>下次检验</span>
      </div>
      <div v-for="d in page.content" :key="d.id" class="device-row clickable" @click="goDetail(d.id)">
        <span class="cell-main">
          <b>{{ d.name }}</b>
          <i>{{ d.deviceCode }}<template v-if="showOrg"> · {{ d.orgName }}</template></i>
        </span>
        <span class="cell" data-label="类型"><StatusBadge :status="d.type" :label="d.typeLabel" /></span>
        <span class="cell" data-label="状态"><StatusBadge :status="d.status" :label="d.statusLabel" /></span>
        <span class="cell" data-label="位置">{{ d.locationMasked }}</span>
        <span class="cell" data-label="下次检验">{{ d.nextInspectionDate || '—' }}</span>
      </div>
    </div>

    <div class="pager" v-if="page.totalPages > 1">
      <button class="btn btn-sm" :disabled="page.page === 0" @click="gotoPage(page.page - 1)">上一页</button>
      <span>{{ page.page + 1 }} / {{ page.totalPages }}</span>
      <button class="btn btn-sm" :disabled="page.page >= page.totalPages - 1" @click="gotoPage(page.page + 1)">下一页</button>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { apiGet } from '../api/client'
import { useAuthStore } from '../stores/auth'
import { useOfflineStore } from '../stores/offline'
import StatusBadge from '../components/StatusBadge.vue'
import ErrorState from '../components/ErrorState.vue'

const router = useRouter()
const auth = useAuthStore()
const offline = useOfflineStore()

const meta = reactive({ statuses: [], types: [] })
const filters = reactive({ keyword: '', status: '', type: '' })
const page = reactive({ content: [], totalElements: 0, totalPages: 0, page: 0, size: 10 })
const loading = ref(false)
const error = ref('')
const errorStatus = ref(500)
const snapshot = ref(false)
const snapshotAt = ref('')

const showOrg = computed(() => !auth.user?.orgName)

function listUrl(p = page.page) {
  const params = new URLSearchParams({ page: String(p), size: String(page.size) })
  if (filters.status) params.set('status', filters.status)
  if (filters.type) params.set('type', filters.type)
  if (filters.keyword) params.set('keyword', filters.keyword)
  return `/devices?${params.toString()}`
}

async function load(p = 0) {
  loading.value = true
  error.value = ''
  try {
    const resp = await apiGet(listUrl(p))
    Object.assign(page, resp.data.data)
    snapshot.value = resp.offline
    if (resp.offline) snapshotAt.value = new Date(resp.cachedAt).toLocaleString('zh-CN', { hour12: false })
  } catch (e) {
    error.value = e.message
    errorStatus.value = e.status || 500
  } finally {
    loading.value = false
  }
}

function search() { load(0) }
function gotoPage(p) { load(p) }
function goDetail(id) { router.push(`/devices/${id}`) }

onMounted(async () => {
  load(0)
  try {
    const resp = await apiGet('/devices/meta')
    meta.statuses = resp.data.data.statuses
    meta.types = resp.data.data.types
  } catch (e) { /* 离线时筛选器使用空配置 */ }
})

watch(() => offline.online, (v) => { if (v) load(page.page) })
</script>
