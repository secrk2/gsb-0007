<template>
  <router-view v-if="isLoginPage" />
  <div v-else class="layout">
    <header class="topbar">
      <div class="brand" @click="$router.push('/')">
        <span class="brand-logo">特安</span>
        <span class="brand-name">特种设备全生命周期监管</span>
      </div>
      <nav class="nav">
        <router-link to="/" exact-active-class="active">设备作战台</router-link>
        <router-link to="/devices" active-class="active">设备档案</router-link>
      </nav>
      <div class="topbar-right">
        <label class="offline-switch" :title="offline.online ? '点击模拟断网（演示外巡检离线场景）' : '点击恢复联网'">
          <input type="checkbox" :checked="!offline.online" @change="offline.toggleManual()" />
          <span>{{ offline.online ? '在线' : '模拟断网' }}</span>
        </label>
        <span v-if="auth.user" class="user-chip">
          {{ auth.user.realName }} · {{ auth.user.roleLabel }}<template v-if="auth.user.orgName"> · {{ auth.user.orgName }}</template>
        </span>
        <button class="btn btn-ghost" @click="onLogout">退出</button>
      </div>
    </header>
    <OfflineBanner />
    <main class="main">
      <router-view />
    </main>
    <footer class="footer">特安监管平台 · 市特种设备检验研究院</footer>
    <div class="toast-wrap">
      <div v-for="t in ui.toasts" :key="t.id" class="toast" :class="t.type">{{ t.message }}</div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from './stores/auth'
import { useOfflineStore } from './stores/offline'
import { useUiStore } from './stores/ui'
import OfflineBanner from './components/OfflineBanner.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const offline = useOfflineStore()
const ui = useUiStore()

const isLoginPage = computed(() => route.name === 'login')

onMounted(() => offline.init())

function onLogout() {
  auth.logout()
  router.push('/login')
}
</script>
