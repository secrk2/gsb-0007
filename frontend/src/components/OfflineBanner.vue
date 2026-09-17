<template>
  <div v-if="!offline.online" class="offline-banner offline">
    <span class="dot"></span>
    当前处于离线状态：页面数据为本地缓存快照（可能已过期），设备状态变更已禁用；
    <b>{{ offline.pendingCount }}</b> 条维保记录待同步。
  </div>
  <div v-else-if="offline.pendingCount > 0" class="offline-banner syncing">
    <span class="dot ok"></span>
    已恢复联网，正在同步 {{ offline.pendingCount }} 条离线记录…
    <button class="btn btn-sm" :disabled="offline.syncing" @click="offline.flush()">
      {{ offline.syncing ? '同步中…' : '立即同步' }}
    </button>
  </div>
  <div v-else-if="offline.lastSyncText" class="offline-banner synced">
    <span class="dot ok"></span>{{ offline.lastSyncText }}
  </div>
</template>

<script setup>
import { useOfflineStore } from '../stores/offline'

const offline = useOfflineStore()
</script>
