<template>
  <div style="min-height:100vh;display:flex;align-items:center;justify-content:center;background:#10203f">
    <div class="card" style="width:400px;max-width:92vw">
      <h2 style="margin:0 0 4px">特安</h2>
      <p class="muted" style="margin:0 0 20px">特种设备检验与维保全生命周期管理平台</p>

      <div v-if="error" class="alert error">{{ error }}</div>

      <div class="field">
        <label>账号</label>
        <input v-model="username" class="input" placeholder="请输入账号" @keyup.enter="submit" />
      </div>
      <div class="field">
        <label>密码</label>
        <input v-model="password" type="password" class="input" placeholder="请输入密码" @keyup.enter="submit" />
      </div>
      <button class="btn primary" style="width:100%;justify-content:center" :disabled="loading" @click="submit">
        {{ loading ? '登录中…' : '登 录' }}
      </button>

      <div class="muted" style="font-size:12px;margin-top:18px;line-height:1.9">
        演示账号（密码均为 123456）：<br />
        设备管理员：wuye_admin / yiyuan_admin / mall_admin<br />
        维保人员：weibao01 · 检验员：jianyan01 · 监察员：jiancha01
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api'
import { useAuthStore } from '../store/auth'

const username = ref('')
const password = ref('')
const error = ref('')
const loading = ref(false)
const router = useRouter()
const auth = useAuthStore()

async function submit() {
  if (!username.value || !password.value) {
    error.value = '请输入账号和密码'
    return
  }
  loading.value = true
  error.value = ''
  try {
    const data = await api.post('/auth/login', { username: username.value, password: password.value })
    auth.login(data.token, data.user)
    router.push('/')
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}
</script>
