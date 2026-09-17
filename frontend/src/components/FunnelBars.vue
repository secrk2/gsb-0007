<template>
  <div class="funnel">
    <div v-for="row in rows" :key="row.key" class="funnel-row">
      <span class="label">{{ row.label }}</span>
      <div class="bar" :style="{ width: barWidth(row.count), background: row.color }">
        <span v-if="row.count > 0"></span>
      </div>
      <span class="count">{{ row.count }}</span>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { STATUS_LABELS } from '../utils/format'

const props = defineProps({ funnel: { type: Object, required: true } })

const COLORS = {
  REGISTERED: '#60a5fa',
  ACCEPTED: '#818cf8',
  IN_USE: '#22c55e',
  SUSPENDED: '#f59e0b',
  SCRAPPED: '#9ca3af'
}

const rows = computed(() =>
  Object.keys(STATUS_LABELS).map((key) => ({
    key,
    label: STATUS_LABELS[key],
    count: props.funnel[key] || 0,
    color: COLORS[key]
  }))
)

const max = computed(() => Math.max(1, ...rows.value.map((r) => r.count)))

function barWidth(count) {
  if (count === 0) return '2px'
  return Math.max(6, Math.round((count / max.value) * 70)) + '%'
}
</script>
