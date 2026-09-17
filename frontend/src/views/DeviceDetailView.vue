<template>
  <div class="page">
    <ErrorState v-if="fatalError" :code="fatalStatus" :title="fatalTitle" :message="fatalError" />

    <template v-else-if="detail">
      <div class="page-head">
        <div>
          <h1>
            {{ detail.device.name }}
            <StatusBadge :status="detail.device.status" :label="detail.device.statusLabel" />
          </h1>
          <p class="page-sub">
            {{ detail.device.deviceCode }} · {{ detail.device.typeLabel }} · {{ detail.device.orgName }}
          </p>
        </div>
        <button class="btn btn-ghost" @click="$router.back()">返回</button>
      </div>

      <div v-if="snapshot" class="snapshot-tip">
        ⚠ 离线快照（{{ snapshotAt }}），数据可能已过期；状态变更已禁用，恢复联网后自动刷新
      </div>

      <div class="detail-grid">
        <!-- 基本信息 -->
        <section class="card">
          <h2>基本信息</h2>
          <dl class="kv">
            <div><dt>注册代码</dt><dd>{{ detail.device.deviceCode }}</dd></div>
            <div><dt>设备类型</dt><dd>{{ detail.device.typeLabel }}</dd></div>
            <div><dt>规格型号</dt><dd>{{ detail.model || '—' }}</dd></div>
            <div><dt>使用单位</dt><dd>{{ detail.device.orgName }}</dd></div>
            <div><dt>位置（已脱敏）</dt><dd>🔒 {{ detail.device.locationMasked }}</dd></div>
            <div><dt>安装日期</dt><dd>{{ detail.installDate || '—' }}</dd></div>
            <div>
              <dt>下次检验日期</dt>
              <dd>
                {{ detail.device.nextInspectionDate || '—' }}
                <span v-if="inspectionChip" class="chip" :class="inspectionChip.cls">{{ inspectionChip.text }}</span>
              </dd>
            </div>
          </dl>
        </section>

        <!-- 精确地址 -->
        <section class="card">
          <h2>精确地址（脱敏保护）</h2>
          <template v-if="!revealed">
            <p class="muted">设备所在建筑与具体位置已脱敏为「区域 · 编号」。精确地址仅监察员填写理由并二次确认后可见，查看行为全程留痕。</p>
            <button v-if="detail.canReveal" class="btn" @click="revealModal = true" :disabled="!offline.online">
              申请查看精确地址
            </button>
            <p v-else class="muted">当前角色无查看权限（仅监察员）。</p>
          </template>
          <template v-else>
            <p class="revealed-addr">📍 {{ revealed.exactAddress }}</p>
            <p class="muted">查看人：{{ revealed.operator }} · {{ revealed.revealedAt }}<br />{{ revealed.notice }}</p>
          </template>
          <template v-if="detail.canReveal && revealLogs.length">
            <h3 class="subhead">查看留痕（{{ revealLogs.length }}）</h3>
            <div class="reveal-log" v-for="(l, i) in revealLogs" :key="i">
              <b>{{ l.realName }}</b>（{{ l.username }}）· {{ fmt(l.createdAt) }} · IP {{ l.ip || '—' }}<br />
              <span class="muted">理由：{{ l.reason }}</span>
            </div>
          </template>
        </section>

        <!-- 状态流转 -->
        <section class="card">
          <h2>状态流转</h2>
          <p class="muted">状态机：注册告知 → 验收 → 在用 → 停用 → 报废（停用可恢复在用，报废为终态）</p>
          <div v-if="transitionError" class="alert-danger">{{ transitionError }}</div>
          <div v-if="transitionDone" class="alert-ok">{{ transitionDone }}</div>
          <div class="transition-actions" v-if="detail.allowedTransitions.length">
            <button v-for="t in detail.allowedTransitions" :key="t.value" class="btn btn-primary"
                    :disabled="!offline.online || !auth.canTransition || transitioning"
                    :title="!offline.online ? '离线状态下不可变更状态' : (!auth.canTransition ? '当前角色无权变更状态' : '')"
                    @click="askTransition(t)">
              变更为「{{ t.label }}」
            </button>
          </div>
          <p v-else class="muted">当前状态为终态或无可用流转。</p>

          <div v-if="pendingTransition" class="confirm-box">
            <p>确认将设备由「{{ detail.device.statusLabel }}」变更为「{{ pendingTransition.label }}」？</p>
            <input v-model.trim="transitionReason" placeholder="变更理由（可选，将写入留痕）" />
            <div>
              <button class="btn btn-primary btn-sm" @click="doTransition">确认变更</button>
              <button class="btn btn-ghost btn-sm" @click="pendingTransition = null">取消</button>
            </div>
          </div>

          <details class="illegal-demo">
            <summary>非法流转演示（状态机拦截并说明原因）</summary>
            <div class="illegal-row">
              <select v-model="illegalTarget">
                <option v-for="s in illegalTargets" :key="s.value" :value="s.value">{{ s.label }}</option>
              </select>
              <button class="btn btn-sm" :disabled="!offline.online || !illegalTarget" @click="tryIllegal">
                尝试变更为该状态
              </button>
            </div>
          </details>
        </section>

        <!-- 状态时间线 -->
        <section class="card">
          <h2>流转留痕</h2>
          <p v-if="!detail.statusLogs.length" class="empty">暂无流转记录</p>
          <div v-for="(l, i) in detail.statusLogs" :key="i" class="timeline-item">
            <span class="timeline-dot"></span>
            <div>
              <b>{{ l.fromLabel }} → {{ l.toLabel }}</b>
              <span class="meta">{{ fmt(l.createdAt) }} · {{ l.operatorName || '系统' }}</span>
              <p class="muted">{{ l.reason }}</p>
            </div>
          </div>
        </section>

        <!-- 维保记录 -->
        <section class="card">
          <h2>维保记录</h2>
          <div v-if="pendingItems.length" class="pending-box">
            <p><b>{{ pendingItems.length }}</b> 条离线记录待同步（恢复联网后自动合并，幂等去重）：</p>
            <div v-for="p in pendingItems" :key="p.recordNo" class="pending-item">
              <span class="chip warn">待同步</span> {{ p.content }}
            </div>
          </div>
          <form class="maint-form" @submit.prevent="submitMaintenance">
            <textarea v-model.trim="maintContent" rows="2" maxlength="500"
                      placeholder="登记维保内容（离线时可正常登记，进入待同步队列）"></textarea>
            <button class="btn btn-primary btn-sm" :disabled="!maintContent || submitting">
              {{ offline.online ? '提交维保记录' : '离线登记（待同步）' }}
            </button>
          </form>
          <p v-if="!maintenance.length" class="empty">暂无维保记录</p>
          <div v-for="m in maintenance" :key="m.recordNo" class="maint-item">
            <div>
              <b>{{ m.maintainerName }}</b>
              <span class="meta">{{ fmt(m.clientCreatedAt || m.createdAt) }}</span>
              <span v-if="m.offline" class="chip">离线补录</span>
            </div>
            <p>{{ m.content }}</p>
          </div>
        </section>

        <!-- 隐患 -->
        <section class="card">
          <h2>隐患</h2>
          <p v-if="!hazards.length" class="empty">暂无隐患记录</p>
          <div v-for="h in hazards" :key="h.id" class="hazard-item" :class="{ overdue: h.overdue }">
            <div class="hazard-line">
              <StatusBadge :status="h.level" :label="h.levelLabel" />
              <StatusBadge :status="h.status" :label="h.statusLabel" />
              <span v-if="h.overdue" class="chip danger">已逾期</span>
            </div>
            <b>{{ h.title }}</b>
            <span class="meta">整改期限：{{ h.deadline || '—' }}</span>
            <button v-if="h.status === 'OPEN' && auth.canResolveHazard" class="btn btn-sm"
                    :disabled="!offline.online" @click="resolveHazard(h.id)">确认闭环</button>
          </div>
        </section>
      </div>

      <!-- 精确地址二次确认弹窗 -->
      <div v-if="revealModal" class="modal-mask" @click.self="revealModal = false">
        <div class="modal">
          <h3>查看精确地址 · 二次确认</h3>
          <p class="muted">该操作将解除位置脱敏，查看人、理由、时间与 IP 将写入审计留痕。</p>
          <textarea v-model.trim="revealReason" rows="3" maxlength="200"
                    placeholder="请填写查看理由（必填，如：专项监察抽查）"></textarea>
          <label class="confirm-check">
            <input type="checkbox" v-model="revealConfirm" />
            我已知晓查看行为将被记录，确认查看
          </label>
          <p v-if="revealError" class="alert-danger">{{ revealError }}</p>
          <div class="modal-actions">
            <button class="btn btn-primary" :disabled="revealing" @click="doReveal">
              {{ revealing ? '提交中…' : '确认查看' }}
            </button>
            <button class="btn btn-ghost" @click="revealModal = false">取消</button>
          </div>
        </div>
      </div>
    </template>

    <div v-else class="loading">加载中…</div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { api, apiGet } from '../api/client'
import { useAuthStore } from '../stores/auth'
import { useOfflineStore } from '../stores/offline'
import { useUiStore } from '../stores/ui'
import StatusBadge from '../components/StatusBadge.vue'
import ErrorState from '../components/ErrorState.vue'

const route = useRoute()
const auth = useAuthStore()
const offline = useOfflineStore()
const ui = useUiStore()
const deviceId = route.params.id

const detail = ref(null)
const hazards = ref([])
const maintenance = ref([])
const revealLogs = ref([])
const revealed = ref(null)
const fatalError = ref('')
const fatalStatus = ref(500)
const fatalTitle = ref('加载失败')
const snapshot = ref(false)
const snapshotAt = ref('')

const transitionError = ref('')
const transitionDone = ref('')
const pendingTransition = ref(null)
const transitionReason = ref('')
const transitioning = ref(false)
const illegalTarget = ref('')

const revealModal = ref(false)
const revealReason = ref('')
const revealConfirm = ref(false)
const revealError = ref('')
const revealing = ref(false)

const maintContent = ref('')
const submitting = ref(false)

const ALL_STATUSES = [
  { value: 'REGISTERED', label: '注册告知' },
  { value: 'ACCEPTED', label: '验收' },
  { value: 'IN_USE', label: '在用' },
  { value: 'SUSPENDED', label: '停用' },
  { value: 'SCRAPPED', label: '报废' }
]

const illegalTargets = computed(() => {
  const allowed = new Set((detail.value?.allowedTransitions || []).map((t) => t.value))
  return ALL_STATUSES.filter((s) => !allowed.has(s.value) && s.value !== detail.value?.device.status)
})

const pendingItems = computed(() =>
  offline.queue.filter((q) => String(q.deviceId) === String(deviceId)))

const inspectionChip = computed(() => {
  const date = detail.value?.device.nextInspectionDate
  if (!date || detail.value?.device.status === 'SCRAPPED') return null
  const days = Math.ceil((new Date(date) - new Date(new Date().toDateString())) / 86400000)
  if (days < 0) return { cls: 'danger', text: `已逾期 ${-days} 天` }
  if (days <= 30) return { cls: 'warn', text: `临期 · 剩 ${days} 天` }
  return null
})

function fmt(t) {
  if (!t) return '—'
  return String(t).replace('T', ' ').slice(0, 16)
}

async function load() {
  try {
    const resp = await apiGet(`/devices/${deviceId}`)
    detail.value = resp.data.data
    snapshot.value = resp.offline
    if (resp.offline) snapshotAt.value = new Date(resp.cachedAt).toLocaleString('zh-CN', { hour12: false })
  } catch (e) {
    fatalStatus.value = e.status || 500
    fatalError.value = e.message
    fatalTitle.value = e.status === 403 ? '越权访问已拦截' : (e.status === 404 ? '设备不存在' : '加载失败')
    return
  }
  loadExtras()
}

async function loadExtras() {
  try {
    const hz = await apiGet(`/hazards/device/${deviceId}`)
    hazards.value = hz.data.data
  } catch (e) { /* 离线无缓存时忽略 */ }
  try {
    const mt = await apiGet(`/maintenance?deviceId=${deviceId}`)
    maintenance.value = mt.data.data
  } catch (e) { /* 同上 */ }
  if (detail.value?.canReveal) {
    try {
      const rv = await apiGet(`/devices/${deviceId}/address/reveals`)
      revealLogs.value = rv.data.data
    } catch (e) { /* 忽略 */ }
  }
}

function askTransition(t) {
  transitionError.value = ''
  transitionDone.value = ''
  transitionReason.value = ''
  pendingTransition.value = t
}

async function doTransition() {
  if (!pendingTransition.value) return
  transitioning.value = true
  transitionError.value = ''
  try {
    const { data } = await api.post(`/devices/${deviceId}/transition`, {
      toStatus: pendingTransition.value.value,
      reason: transitionReason.value
    })
    detail.value = data.data
    transitionDone.value = `已变更为「${data.data.device.statusLabel}」，留痕已记录`
    pendingTransition.value = null
  } catch (e) {
    transitionError.value = e.message
    pendingTransition.value = null
  } finally {
    transitioning.value = false
  }
}

async function tryIllegal() {
  transitionError.value = ''
  transitionDone.value = ''
  try {
    await api.post(`/devices/${deviceId}/transition`, { toStatus: illegalTarget.value, reason: '非法流转演示' })
  } catch (e) {
    transitionError.value = `已拦截（HTTP ${e.status}）：${e.message}`
  }
}

async function doReveal() {
  revealError.value = ''
  revealing.value = true
  try {
    const { data } = await api.post(`/devices/${deviceId}/address/reveal`, {
      reason: revealReason.value,
      confirm: revealConfirm.value
    })
    revealed.value = data.data
    revealModal.value = false
    ui.success('已留痕，精确地址可见')
    const rv = await api.get(`/devices/${deviceId}/address/reveals`)
    revealLogs.value = rv.data.data
  } catch (e) {
    revealError.value = e.message
  } finally {
    revealing.value = false
  }
}

async function submitMaintenance() {
  const content = maintContent.value
  maintContent.value = ''
  if (!offline.online) {
    offline.enqueueMaintenance({ deviceId: Number(deviceId), content })
    ui.toast('已加入离线队列，恢复联网后自动同步（幂等）')
    return
  }
  submitting.value = true
  try {
    await api.post('/maintenance/sync', {
      items: [{
        recordNo: crypto.randomUUID(),
        deviceId: Number(deviceId),
        content,
        clientCreatedAt: new Date().toISOString().slice(0, 19),
        offline: false
      }]
    })
    ui.success('维保记录已提交')
    const mt = await api.get(`/maintenance?deviceId=${deviceId}`)
    maintenance.value = mt.data.data
  } catch (e) {
    ui.error(e.message)
    maintContent.value = content
  } finally {
    submitting.value = false
  }
}

async function resolveHazard(id) {
  try {
    await api.post(`/hazards/${id}/resolve`)
    ui.success('隐患已闭环')
    loadExtras()
  } catch (e) {
    ui.error(e.message)
  }
}

onMounted(load)
watch(() => offline.online, async (v) => {
  if (v) {
    await offline.flush()
    load()
  }
})
</script>
