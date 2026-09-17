<template>
  <div>
    <h1 class="page-title">设备作战台</h1>
    <p class="page-sub">
      各使用单位在用设备漏斗、检验临期与隐患逾期一览
      <span v-if="battle" class="muted">· 数据生成于 {{ fmtTime(battle.generatedAt) }}</span>
      <span v-if="cached" class="tag-cache">离线缓存 {{ fmtClock(cachedAt) }}</span>
    </p>

    <div v-if="error" class="alert error">{{ error }}</div>
    <div v-if="!battle && !error" class="muted">加载中…</div>

    <div v-if="battle" class="grid" :class="battle.orgs.length > 1 ? 'grid-3' : ''">
      <div v-for="org in battle.orgs" :key="org.orgId" class="card">
        <h3 class="card-title">
          {{ org.orgName }}
          <span v-if="org.overdueHazards > 0 || org.overdueInspections.length > 0" class="red-dot" title="存在逾期项"></span>
        </h3>

        <FunnelBars :funnel="org.funnel" />

        <div style="display:flex;gap:16px;margin-top:16px;flex-wrap:wrap">
          <div>
            <div class="muted" style="font-size:12px">检验临期（{{ battle.dueDays }}天内）</div>
            <div style="font-size:22px;font-weight:700" :style="org.dueSoon.length ? 'color:var(--warn)' : ''">
              {{ org.dueSoon.length }}
            </div>
          </div>
          <div>
            <div class="muted" style="font-size:12px">检验逾期</div>
            <div style="font-size:22px;font-weight:700" :style="org.overdueInspections.length ? 'color:var(--danger)' : ''">
              {{ org.overdueInspections.length }}
            </div>
          </div>
          <div>
            <div class="muted" style="font-size:12px">未闭环隐患</div>
            <div style="font-size:22px;font-weight:700" :style="org.openHazards ? 'color:var(--warn)' : ''">
              {{ org.openHazards }}
            </div>
          </div>
          <div>
            <div class="muted" style="font-size:12px">隐患逾期</div>
            <div style="font-size:22px;font-weight:700" :style="org.overdueHazards ? 'color:var(--danger)' : ''">
              {{ org.overdueHazards }}
            </div>
          </div>
        </div>

        <div v-if="org.overdueInspections.length" style="margin-top:14px">
          <div class="muted" style="font-size:12px;margin-bottom:6px"><span class="red-dot"></span>检验逾期设备</div>
          <div v-for="d in org.overdueInspections" :key="d.id" class="alert error" style="margin-bottom:6px;padding:6px 10px">
            <router-link :to="`/devices/${d.id}`">{{ d.name }}</router-link>
            <span class="muted">（{{ d.typeLabel }} · {{ d.maskedLocation }}）</span>
            应于 {{ fmtDate(d.nextInspectionDate) }} 前检验
          </div>
        </div>

        <div v-if="org.dueSoon.length" style="margin-top:10px">
          <div class="muted" style="font-size:12px;margin-bottom:6px">临期设备</div>
          <div v-for="d in org.dueSoon" :key="d.id" class="alert warn" style="margin-bottom:6px;padding:6px 10px">
            <router-link :to="`/devices/${d.id}`">{{ d.name }}</router-link>
            <span class="muted">（{{ d.typeLabel }} · {{ d.maskedLocation }}）</span>
            {{ fmtDate(d.nextInspectionDate) }} 到期
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { cachedGet } from '../api'
import FunnelBars from '../components/FunnelBars.vue'
import { fmtDate, fmtTime, fmtClock } from '../utils/format'

const battle = ref(null)
const cached = ref(false)
const cachedAt = ref(null)
const error = ref('')

onMounted(async () => {
  try {
    const res = await cachedGet('/dashboard/battle')
    battle.value = res.data
    cached.value = res.cached
    cachedAt.value = res.cachedAt
  } catch (e) {
    error.value = e.network ? '当前离线且暂无缓存数据，请联网后查看' : e.message
  }
})
</script>
