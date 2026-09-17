<template>
  <div>
    <h1 class="page-title">维保巡检</h1>
    <p class="page-sub">外巡检维保登记；断网时记录暂存本地，恢复后自动合并，不重复、不丢失</p>

    <div v-if="!offline.online" class="alert warn">
      当前离线：登记内容将暂存本机（{{ offline.queue.length }} 条待同步），恢复网络后自动合并到服务端。
    </div>
    <div v-if="offline.lastSync" class="alert ok">
      上次同步完成：新增 {{ offline.lastSync.created }} 条，合并去重 {{ offline.lastSync.duplicate }} 条。
    </div>
    <div v-if="message" class="alert" :class="messageType">{{ message }}</div>

    <div class="grid grid-2">
      <div class="card">
        <h3 class="card-title">登记维保记录</h3>
        <div class="field">
          <label>设备</label>
          <select v-model="form.deviceId" class="input">
            <option value="" disabled>请选择设备</option>
            <option v-for="d in devices" :key="d.id" :value="d.id">
              {{ d.code }} · {{ d.name }}（{{ d.maskedLocation }}）
            </option>
          </select>
        </div>
        <div class="field">
          <label>维保时间</label>
          <input v-model="form.happenedAt" type="datetime-local" class="input" />
        </div>
        <div class="field">
          <label>维保内容</label>
          <textarea v-model="form.content" class="input" rows="4"
                    placeholder="如：半月保——机房清洁、曳引机检查、层门门锁测试"></textarea>
        </div>
        <button class="btn primary" :disabled="submitting" @click="submit">
          {{ offline.online ? '提交' : '离线暂存' }}
        </button>
      </div>

      <div>
        <div class="card">
          <h3 class="card-title">本地待同步队列（{{ offline.queue.length }}）</h3>
          <div v-if="!offline.queue.length" class="muted">无待同步记录</div>
          <div v-for="q in offline.queue" :key="q.clientId" style="padding:6px 0;border-bottom:1px solid var(--line)">
            <div>{{ q.content }}</div>
            <div class="muted" style="font-size:12px">设备 #{{ q.deviceId }} · {{ fmtTime(q.happenedAt) }}</div>
          </div>
          <button v-if="offline.queue.length && offline.online" class="btn sm"
                  style="margin-top:10px" @click="syncNow">立即同步</button>
        </div>

        <div class="card" v-if="recent.length">
          <h3 class="card-title">所选设备近期维保</h3>
          <div v-for="m in recent" :key="m.id" style="padding:6px 0;border-bottom:1px solid var(--line)">
            <div>{{ m.content }} <span v-if="m.offline" class="tag-cache">离线补录</span></div>
            <div class="muted" style="font-size:12px">{{ m.maintainerName }} · {{ fmtTime(m.happenedAt) }}</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import api, { cachedGet } from '../api'
import { useOfflineStore } from '../store/offline'
import { fmtTime, uuid } from '../utils/format'

const offline = useOfflineStore()
const devices = ref([])
const recent = ref([])
const message = ref('')
const messageType = ref('ok')
const submitting = ref(false)

const now = new Date()
const pad = (n) => String(n).padStart(2, '0')
const form = reactive({
  deviceId: '',
  happenedAt: `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}T${pad(now.getHours())}:${pad(now.getMinutes())}`,
  content: ''
})

onMounted(async () => {
  try {
    const res = await cachedGet('/devices', { params: { size: 100 } })
    devices.value = res.data.content
  } catch (e) {
    // 离线无缓存时给出提示而非空白
    message.value = e.message
    messageType.value = 'error'
  }
})

watch(() => form.deviceId, async (id) => {
  if (!id || !offline.online) return
  try {
    recent.value = await api.get('/maintenance', { params: { deviceId: id } })
  } catch (e) {
    recent.value = []
  }
})

async function submit() {
  message.value = ''
  if (!form.deviceId || !form.content.trim()) {
    message.value = '请选择设备并填写维保内容'
    messageType.value = 'error'
    return
  }
  const record = {
    clientId: uuid(),
    deviceId: Number(form.deviceId),
    content: form.content.trim(),
    happenedAt: form.happenedAt,
    offline: !offline.online
  }
  submitting.value = true
  try {
    if (offline.online) {
      const result = await api.post('/maintenance', record)
      message.value = result.status === 'DUPLICATE' ? '该记录已提交过，已自动去重' : '提交成功'
      messageType.value = 'ok'
    } else {
      offline.enqueue(record)
      message.value = '已离线暂存，恢复网络后自动同步'
      messageType.value = 'warn'
    }
    form.content = ''
  } catch (e) {
    if (e.network) {
      // 提交瞬间掉线：转入本地队列，不丢单
      offline.enqueue({ ...record, offline: true })
      message.value = '网络中断，已转入本地队列'
      messageType.value = 'warn'
    } else {
      message.value = e.message
      messageType.value = 'error'
    }
  } finally {
    submitting.value = false
  }
}

async function syncNow() {
  await offline.flush()
  if (offline.lastSync) {
    message.value = `同步完成：新增 ${offline.lastSync.created} 条，去重 ${offline.lastSync.duplicate} 条`
    messageType.value = 'ok'
  }
}
</script>
