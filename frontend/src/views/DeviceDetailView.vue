<template>
  <div>
    <!-- 错误态：越权/不存在时明确提示，不留空白页 -->
    <div v-if="fatalError" class="card error-state">
      <div class="code">{{ fatalCode }}</div>
      <div class="msg">{{ fatalError }}</div>
      <button class="btn" @click="$router.push('/devices')">返回设备档案</button>
    </div>

    <template v-else-if="device">
      <div style="display:flex;align-items:center;gap:12px;flex-wrap:wrap;margin-bottom:4px">
        <h1 class="page-title" style="margin:0">{{ device.name }}</h1>
        <StatusBadge :status="device.status" />
        <span v-if="cached" class="tag-cache">离线缓存 {{ fmtClock(cachedAt) }}</span>
      </div>
      <p class="page-sub mono">{{ device.code }} · {{ device.typeLabel }} · {{ device.orgName }}</p>

      <div v-if="!offline.online" class="alert warn">
        当前离线：状态变更与地址查看不可用，避免基于过期状态误操作。
      </div>
      <div v-if="actionError" class="alert error">{{ actionError }}</div>
      <div v-if="actionOk" class="alert ok">{{ actionOk }}</div>

      <div class="grid grid-2">
        <div class="card">
          <h3 class="card-title">档案信息</h3>
          <table class="data">
            <tbody>
              <tr><td class="muted" style="width:110px;cursor:default">设备编号</td><td class="mono">{{ device.code }}</td></tr>
              <tr><td class="muted">设备类型</td><td>{{ device.typeLabel }}</td></tr>
              <tr><td class="muted">当前状态</td><td><StatusBadge :status="device.status" /></td></tr>
              <tr><td class="muted">使用单位</td><td>{{ device.orgName }}</td></tr>
              <tr>
                <td class="muted">所在位置</td>
                <td>
                  🔒 {{ device.maskedLocation }}
                  <span class="muted" style="font-size:12px">（精确地址已脱敏）</span>
                  <button
                    v-if="device.canRevealAddress"
                    class="btn sm"
                    style="margin-left:8px"
                    :disabled="!offline.online"
                    @click="revealModal = true"
                  >查看精确地址</button>
                </td>
              </tr>
              <tr><td class="muted">上次检验</td><td class="mono">{{ fmtDate(device.lastInspectionDate) }}</td></tr>
              <tr><td class="muted">下次检验</td><td class="mono">{{ fmtDate(device.nextInspectionDate) }}</td></tr>
              <tr>
                <td class="muted">未闭环隐患</td>
                <td>
                  <span v-if="device.openHazards > 0"><span class="red-dot"></span>{{ device.openHazards }} 项</span>
                  <span v-else class="muted">无</span>
                </td>
              </tr>
            </tbody>
          </table>

          <template v-if="revealed">
            <div class="alert info" style="margin-top:12px">
              <div><b>{{ revealed.buildingName }}</b></div>
              <div>{{ revealed.exactAddress }}</div>
              <div class="muted" style="font-size:12px">本次查看已留痕，请妥善使用</div>
            </div>
          </template>

          <template v-if="device.allowedTransitions.length">
            <h3 class="card-title" style="margin-top:18px">状态操作</h3>
            <div class="transitions">
              <button
                v-for="t in device.allowedTransitions"
                :key="t.to"
                class="btn"
                :class="t.to === 'SCRAPPED' ? 'danger' : 'primary'"
                :disabled="!offline.online"
                @click="openTransition(t)"
              >{{ transitionVerb(t.to) }}</button>
            </div>
          </template>
        </div>

        <div>
          <div class="card">
            <h3 class="card-title">状态流转记录</h3>
            <ul v-if="history.length" class="timeline">
              <li v-for="h in history" :key="h.id">
                <div>
                  <StatusBadge :status="h.fromStatus" /> → <StatusBadge :status="h.toStatus" />
                </div>
                <div class="muted" style="font-size:12px">
                  {{ h.operatorName }} · {{ fmtTime(h.createdAt) }}<span v-if="h.reason"> · {{ h.reason }}</span>
                </div>
              </li>
            </ul>
            <div v-else class="muted">暂无流转记录</div>
          </div>

          <div class="card">
            <h3 class="card-title">隐患记录</h3>
            <div v-if="!hazards.length" class="muted">暂无隐患</div>
            <div v-for="h in hazards" :key="h.id" class="alert"
                 :class="h.status === 'RECTIFIED' ? 'ok' : (isOverdue(h) ? 'error' : 'warn')"
                 style="padding:8px 12px">
              <b v-if="h.level === 'MAJOR'">【重大】</b>{{ h.title }}
              <div class="muted" style="font-size:12px">
                整改期限 {{ fmtDate(h.deadline) }}
                <span v-if="h.status === 'RECTIFIED'"> · 已闭环 {{ fmtTime(h.rectifiedAt) }}</span>
                <span v-else-if="isOverdue(h)" style="color:var(--danger)"> · 已逾期</span>
              </div>
              <button v-if="h.status === 'OPEN' && offline.online" class="btn sm" style="margin-top:6px"
                      @click="rectify(h)">标记整改完成</button>
            </div>
          </div>

          <div class="card">
            <h3 class="card-title">检验记录</h3>
            <div v-if="!inspections.length" class="muted">暂无检验记录</div>
            <div v-for="r in inspections" :key="r.id" style="padding:6px 0;border-bottom:1px solid var(--line)">
              <span class="badge" :class="r.result === 'PASS' ? 'IN_USE' : (r.result === 'FAIL' ? 'danger' : 'SUSPENDED')">
                {{ { PASS: '合格', RECTIFY: '整改后复检', FAIL: '不合格' }[r.result] }}
              </span>
              <span class="mono" style="margin-left:8px">{{ fmtDate(r.inspectedAt) }}</span>
              <span class="muted"> · {{ r.inspectorName }} · {{ r.reportNo || '—' }}</span>
            </div>
          </div>

          <div class="card">
            <h3 class="card-title">维保记录</h3>
            <div v-if="!maintenance.length" class="muted">暂无维保记录</div>
            <div v-for="m in maintenance" :key="m.id" style="padding:6px 0;border-bottom:1px solid var(--line)">
              <div>{{ m.content }} <span v-if="m.offline" class="tag-cache">离线补录</span></div>
              <div class="muted" style="font-size:12px">{{ m.maintainerName }} · {{ fmtTime(m.happenedAt) }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 状态流转确认 -->
      <div v-if="transitionModal" class="modal-mask" @click.self="transitionModal = null">
        <div class="modal">
          <h3>确认状态变更</h3>
          <p>
            将设备由 <StatusBadge :status="device.status" /> 变更为
            <StatusBadge :status="transitionModal.to" />
          </p>
          <div class="field">
            <label>变更事由</label>
            <textarea v-model="transitionReason" class="input" rows="3" placeholder="请填写变更事由"></textarea>
          </div>
          <div class="actions">
            <button class="btn" @click="transitionModal = null">取消</button>
            <button class="btn primary" :disabled="submitting" @click="doTransition">确认变更</button>
          </div>
        </div>
      </div>

      <!-- 监察员查看精确地址：二次确认 + 理由 -->
      <div v-if="revealModal" class="modal-mask" @click.self="revealModal = false">
        <div class="modal">
          <h3>查看精确地址</h3>
          <div class="alert warn">设备精确位置属敏感信息，查看将记录操作人、理由与时间，请确认工作需要。</div>
          <div class="field">
            <label>查看理由（不少于 4 个字）</label>
            <textarea v-model="revealReason" class="input" rows="3" placeholder="如：现场监察执法，需定位设备"></textarea>
          </div>
          <div class="field">
            <label style="display:flex;gap:8px;align-items:center;color:var(--ink)">
              <input type="checkbox" v-model="revealConfirm" /> 我确认因工作需要查看，并知晓本次查看将被留痕
            </label>
          </div>
          <div v-if="revealError" class="alert error">{{ revealError }}</div>
          <div class="actions">
            <button class="btn" @click="revealModal = false">取消</button>
            <button class="btn primary" :disabled="submitting" @click="doReveal">确认查看</button>
          </div>
        </div>
      </div>
    </template>

    <div v-else class="muted">加载中…</div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import api, { cachedGet } from '../api'
import StatusBadge from '../components/StatusBadge.vue'
import { useOfflineStore } from '../store/offline'
import { fmtDate, fmtTime, fmtClock } from '../utils/format'

const route = useRoute()
const offline = useOfflineStore()
const id = route.params.id

const device = ref(null)
const history = ref([])
const hazards = ref([])
const inspections = ref([])
const maintenance = ref([])
const cached = ref(false)
const cachedAt = ref(null)
const fatalError = ref('')
const fatalCode = ref('')
const actionError = ref('')
const actionOk = ref('')

const transitionModal = ref(null)
const transitionReason = ref('')
const revealModal = ref(false)
const revealReason = ref('')
const revealConfirm = ref(false)
const revealError = ref('')
const revealed = ref(null)
const submitting = ref(false)

const TRANSITION_VERBS = {
  ACCEPTED: '验收通过',
  IN_USE: '投入使用 / 启用',
  SUSPENDED: '停用',
  SCRAPPED: '报废'
}

function transitionVerb(to) {
  return TRANSITION_VERBS[to] || `变更为${to}`
}

function isOverdue(h) {
  return h.status === 'OPEN' && h.deadline < new Date().toISOString().slice(0, 10)
}

async function load() {
  try {
    const res = await cachedGet(`/devices/${id}`)
    device.value = res.data
    cached.value = res.cached
    cachedAt.value = res.cachedAt
  } catch (e) {
    fatalCode.value = e.code === 403 ? '403' : e.code === 404 ? '404' : '错误'
    fatalError.value = e.network ? '当前离线且暂无该设备的缓存数据' : e.message
    return
  }
  // 关联数据失败不阻塞主档案
  const safe = (p) => p.catch(() => [])
  const [h, hz, insp, mnt] = await Promise.all([
    safe(api.get(`/devices/${id}/history`)),
    safe(api.get('/hazards', { params: { deviceId: id } })),
    safe(api.get('/inspections', { params: { deviceId: id } })),
    safe(api.get('/maintenance', { params: { deviceId: id } }))
  ])
  history.value = h
  hazards.value = hz
  inspections.value = insp
  maintenance.value = mnt
}

function openTransition(t) {
  transitionReason.value = ''
  actionError.value = ''
  actionOk.value = ''
  transitionModal.value = t
}

async function doTransition() {
  submitting.value = true
  actionError.value = ''
  try {
    device.value = await api.post(`/devices/${id}/transition`, {
      to: transitionModal.value.to,
      reason: transitionReason.value
    })
    transitionModal.value = null
    actionOk.value = '状态变更成功'
    const h = await api.get(`/devices/${id}/history`)
    history.value = h
  } catch (e) {
    // 非法流转：后端返回具体原因
    actionError.value = e.message
    transitionModal.value = null
  } finally {
    submitting.value = false
  }
}

async function doReveal() {
  revealError.value = ''
  submitting.value = true
  try {
    revealed.value = await api.post(`/devices/${id}/address/reveal`, {
      reason: revealReason.value,
      confirm: revealConfirm.value
    })
    revealModal.value = false
  } catch (e) {
    revealError.value = e.message
  } finally {
    submitting.value = false
  }
}

async function rectify(h) {
  try {
    await api.post(`/hazards/${h.id}/rectify`)
    hazards.value = await api.get('/hazards', { params: { deviceId: id } })
    device.value = await api.get(`/devices/${id}`)
  } catch (e) {
    actionError.value = e.message
  }
}

onMounted(load)
</script>
