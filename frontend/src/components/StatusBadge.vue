<template>
  <span class="badge" :class="status">{{ label }}</span>
</template>

<script setup>
import { computed } from 'vue'
import { STATUS_LABELS } from '../utils/format'

const props = defineProps({ status: String })
const label = computed(() => STATUS_LABELS[props.status] || props.status)
</script>
