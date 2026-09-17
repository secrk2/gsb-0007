<template>
  <span class="status-badge" :class="statusClass">{{ label }}</span>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  status: { type: String, required: true },
  label: { type: String, default: '' }
})

const LABELS = {
  REGISTERED: '注册告知', ACCEPTED: '验收', IN_USE: '在用',
  SUSPENDED: '停用', SCRAPPED: '报废',
  ELEVATOR: '电梯', CRANE: '起重机械',
  OPEN: '未闭环', RESOLVED: '已闭环',
  HIGH: '重大隐患', MEDIUM: '一般隐患', LOW: '轻微隐患'
}

const label = computed(() => props.label || LABELS[props.status] || props.status)
const statusClass = computed(() => 'st-' + props.status.toLowerCase())
</script>
