<template>
  <div>
    <header class="shell-header">
      <div class="inner">
        <div class="brand">特安<small>特种设备全生命周期管理</small></div>
        <nav class="nav">
          <router-link to="/dashboard" active-class="active">设备作战台</router-link>
          <router-link to="/devices" active-class="active">设备档案</router-link>
          <router-link to="/maintenance" active-class="active">维保巡检</router-link>
        </nav>
        <div class="user-chip">
          <span class="name">{{ auth.user?.name }}</span>
          <span class="role">{{ auth.roleLabel }}</span>
          <span class="name muted" style="color:#9db2d6">{{ auth.user?.orgName }}</span>
          <button class="link-btn" @click="logout">退出</button>
        </div>
      </div>
    </header>

    <div v-if="!offline.online" class="offline-banner">
      <span class="dot"></span>
      <span>当前处于离线状态：页面展示为缓存数据，状态变更不可用；维保记录将暂存本地，恢复网络后自动合并同步。</span>
      <span v-if="offline.queue.length">待同步 {{ offline.queue.length }} 条</span>
    </div>
    <div v-else-if="offline.queue.length" class="offline-banner" style="background:#7c5a12;color:#fef3c7">
      <span class="dot" style="background:#fbbf24"></span>
      <span>有 {{ offline.queue.length }} 条离线维保记录待同步</span>
      <button class="btn sm" @click="offline.flush()">立即同步</button>
    </div>

    <main class="container page">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { useAuthStore } from '../store/auth'
import { useOfflineStore } from '../store/offline'
import { useRouter } from 'vue-router'
import api from '../api'

const auth = useAuthStore()
const offline = useOfflineStore()
const router = useRouter()

async function logout() {
  try { await api.post('/auth/logout') } catch (e) { /* 忽略 */ }
  auth.logout()
  router.push('/login')
}
</script>
