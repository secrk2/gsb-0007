<template>
  <div>
    <h1 class="page-title">设备档案</h1>
    <p class="page-sub">注册告知 → 验收 → 在用 ⇄ 停用 → 报废 全生命周期档案</p>

    <div class="card">
      <div class="filters">
        <div class="field">
          <label>状态</label>
          <select v-model="filters.status" class="input" @change="load(0)">
            <option value="">全部</option>
            <option v-for="(label, key) in STATUS_LABELS" :key="key" :value="key">{{ label }}</option>
          </select>
        </div>
        <div class="field">
          <label>类型</label>
          <select v-model="filters.type" class="input" @change="load(0)">
            <option value="">全部</option>
            <option value="ELEVATOR">电梯</option>
            <option value="CRANE">起重机械</option>
          </select>
        </div>
        <div class="field" style="flex:1">
          <label>关键字</label>
          <input v-model="filters.keyword" class="input" placeholder="设备编号 / 名称" @keyup.enter="load(0)" />
        </div>
        <button class="btn primary" @click="load(0)">查询</button>
      </div>
    </div>

    <div class="card">
      <div v-if="error" class="alert error">{{ error }}</div>
      <div v-if="cached" class="alert warn">当前离线，以下为 {{ fmtClock(cachedAt) }} 的缓存数据，状态可能已变化</div>

      <table v-if="page" class="data">
        <thead>
          <tr>
            <th>设备编号</th><th>名称</th><th>类型</th><th>状态</th>
            <th>使用单位</th><th>位置（脱敏）</th><th>下次检验</th><th>隐患</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="d in page.content" :key="d.id" @click="goDetail(d)">
            <td data-th="设备编号" class="mono">{{ d.code }}</td>
            <td data-th="名称">{{ d.name }}</td>
            <td data-th="类型">{{ d.typeLabel }}</td>
            <td data-th="状态"><StatusBadge :status="d.status" /></td>
            <td data-th="使用单位">{{ d.orgName }}</td>
            <td data-th="位置">{{ d.maskedLocation }}</td>
            <td data-th="下次检验" class="mono">{{ fmtDate(d.nextInspectionDate) }}</td>
            <td data-th="隐患">
              <span v-if="d.openHazards > 0"><span class="red-dot"></span>{{ d.openHazards }}</span>
              <span v-else class="muted">—</span>
            </td>
          </tr>
          <tr v-if="page.content.length === 0">
            <td colspan="8" class="muted" style="text-align:center;cursor:default">暂无设备</td>
          </tr>
        </tbody>
      </table>

      <div v-if="page && page.totalPages > 1" style="margin-top:14px;display:flex;gap:10px;align-items:center">
        <button class="btn sm" :disabled="page.number === 0" @click="load(page.number - 1)">上一页</button>
        <span class="muted">{{ page.number + 1 }} / {{ page.totalPages }}</span>
        <button class="btn sm" :disabled="page.number >= page.totalPages - 1" @click="load(page.number + 1)">下一页</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { cachedGet } from '../api'
import StatusBadge from '../components/StatusBadge.vue'
import { STATUS_LABELS, fmtDate, fmtClock } from '../utils/format'

const router = useRouter()
const filters = reactive({ status: '', type: '', keyword: '' })
const page = ref(null)
const error = ref('')
const cached = ref(false)
const cachedAt = ref(null)

async function load(pageNo = 0) {
  error.value = ''
  try {
    const res = await cachedGet('/devices', {
      params: {
        status: filters.status || undefined,
        type: filters.type || undefined,
        keyword: filters.keyword || undefined,
        page: pageNo,
        size: 20
      }
    })
    page.value = res.data
    cached.value = res.cached
    cachedAt.value = res.cachedAt
  } catch (e) {
    error.value = e.network ? '当前离线且暂无缓存数据' : e.message
  }
}

function goDetail(d) {
  router.push(`/devices/${d.id}`)
}

onMounted(() => load(0))
</script>
