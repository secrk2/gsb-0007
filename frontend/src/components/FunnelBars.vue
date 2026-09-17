<template>
  <div class="funnel">
    <div v-for="step in steps" :key="step.key" class="funnel-row">
      <span class="funnel-label">{{ step.label }}</span>
      <div class="funnel-track">
        <div class="funnel-bar" :class="step.key"
             :style="{ width: barWidth(step.value) }">
          <span v-if="step.value > 0" class="funnel-value">{{ step.value }}</span>
        </div>
        <span v-if="step.value === 0" class="funnel-zero">0</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  funnel: { type: Object, required: true }
})

const steps = computed(() => [
  { key: 'registered', label: '注册告知', value: props.funnel.registered },
  { key: 'accepted', label: '验收', value: props.funnel.accepted },
  { key: 'inUse', label: '在用', value: props.funnel.inUse },
  { key: 'suspended', label: '停用', value: props.funnel.suspended },
  { key: 'scrapped', label: '报废', value: props.funnel.scrapped }
])

const max = computed(() => Math.max(1, ...steps.value.map((s) => s.value)))

function barWidth(v) {
  if (v === 0) return '0%'
  return Math.max(12, Math.round((v / max.value) * 100)) + '%'
}
</script>
