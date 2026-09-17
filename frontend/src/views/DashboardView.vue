<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1>设备作战台</h1>
        <p class="page-sub">{{ scopeText }} · 数据更新于 {{ loadedAt }}</p>
      </div>
      <button class="btn" @click="load" :disabled="loading">{{ loading ? '刷新中…' : '刷新' }}</button>
    </div>

    <div v-if="snapshot" class="snapshot-tip">
      ⚠ 离线快照（{{ snapshotAt }}），数据可能已过期，恢复联网后自动更新
    </div>

    <ErrorState v-if="error" :code="errorStatus" title="作战台加载失败" :message="error" />

    <template v-else-if="summary">
      <!-- 总览指标 -->
      <section class="stat-grid">
        <div class="stat-card"><b>{{ summary.totals.devices }}</b><span>设备总数</span></div>
        <div class="stat-card ok"><b>{{ summary.totals.inUse }}</b><span>在用设备</span></div>
        <div class="stat-card warn"><b>{{ summary.totals.dueSoon }}</b><span>检验临期（30天）</span></div>
        <div class="stat-card danger"><b>{{ summary.totals.overdue }}</b><span>检验逾期</span></div>
        <div class="stat-card danger">
          <b>{{ summary.totals.openHazards }}</b>
          <span>未闭环隐患<i v-if="summary.totals.overdueHazards" class="sub-danger">（逾期 {{ summary.totals.overdueHazards }}）</i></span>
        </div>
      </section>

      <!-- 各单位漏斗 -->
      <section class="section">
        <h2>使用单位在用漏斗</h2>
        <div class="org-grid">
          <div v-for="org in summary.orgs" :key="org.orgId" class="org-card">
            <span v-if="org.redDot" class="red-dot" title="存在检验逾期或隐患逾期"></span>
            <div class="org-head">
              <h3>{{ org.orgName }}</h3>
              <div class="org-chips">
                <span v-if="org.dueSoon" class="chip warn">临期 {{ org.dueSoon }}</span>
                <span v-if="org.overdue" class="chip danger">逾期 {{ org.overdue }}</span>
                <span v-if="org.openHazards" class="chip danger">隐患 {{ org.openHazards }}</span>
              </div>
            </div>
            <FunnelBars :funnel="org" />
          </div>
        </div>
      </section>

      <!-- 临期与逾期 -->
      <section class="section two-col">
        <div class="panel">
          <h2>检验临期（30天内）</h2>
          <p v-if="!summary.dueSoon.length" class="empty">暂无临期设备</p>
          <div v-for="d in summary.dueSoon" :key="d.id" class="due-item" @click="goDevice(d.id)">
            <div>
              <b>{{ d.name }}</b>
              <span class="meta">{{ d.orgName }} · {{ d.typeLabel }} · {{ d.deviceCode }}</span>
            </div>
            <span class="chip warn">剩 {{ d.days }} 天</span>
          </div>
        </div>
        <div class="panel">
          <h2>检验逾期</h2>
          <p v-if="!summary.overdue.length" class="empty">暂无逾期设备</p>
          <div v-for="d in summary.overdue" :key="d.id" class="due-item danger" @click="goDevice(d.id)">
            <div>
              <b>{{ d.name }}</b>
              <span class="meta">{{ d.orgName }} · {{ d.typeLabel }} · {{ d.deviceCode }}</span>
            </div>
            <span class="chip danger">逾期 {{ d.days }} 天</span>
          </div>
        </div>
      </section>

      <!-- 隐患 -->
      <section class="section">
        <h2>未闭环隐患</h2>
        <p v-if="!summary.openHazards.length" class="empty">暂无未闭环隐患</p>
        <div class="hazard-grid">
          <div v-for="h in summary.openHazards" :key="h.id" class="hazard-card" :class="{ overdue: h.overdue }"
               @click="goDevice(h.deviceId)">
            <div class="hazard-top">
              <StatusBadge :status="h.level" :label="h.levelLabel" />
              <span v-if="h.overdue" class="chip danger">已逾期</span>
            </div>
            <b>{{ h.title }}</b>
            <span class="meta">{{ h.orgName }} · {{ h.deviceName }}</span>
            <span class="meta">整改期限：{{ h.deadline || '—' }}</span>
          </div>
        </div>
      </section>
    </template>

    <div v-else class="loading">加载中…</div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { apiGet } from '../api/client'
import { useAuthStore } from '../stores/auth'
import { useOfflineStore } from '../stores/offline'
import StatusBadge from '../components/StatusBadge.vue'
import FunnelBars from '../components/FunnelBars.vue'
import ErrorState from '../components/ErrorState.vue'

const router = useRouter()
const auth = useAuthStore()
const offline = useOfflineStore()

const summary = ref(null)
const loading = ref(false)
const error = ref('')
const errorStatus = ref(500)
const snapshot = ref(false)
const snapshotAt = ref('')
const loadedAt = ref('')

const scopeText = computed(() =>
  auth.user?.orgName ? `数据范围：${auth.user.orgName}` : '数据范围：全市使用单位')

function fmtTime(ts) {
  return new Date(ts).toLocaleString('zh-CN', { hour12: false })
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const resp = await apiGet('/dashboard/summary')
    summary.value = resp.data.data
    snapshot.value = resp.offline
    if (resp.offline) snapshotAt.value = fmtTime(resp.cachedAt)
    loadedAt.value = fmtTime(Date.now())
  } catch (e) {
    error.value = e.message
    errorStatus.value = e.status || 500
  } finally {
    loading.value = false
  }
}

function goDevice(id) {
  router.push(`/devices/${id}`)
}

onMounted(load)
// 恢复联网后自动刷新，避免一直看旧快照
watch(() => offline.online, (v) => { if (v) load() })
</script>
