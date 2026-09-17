<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-brand">
        <span class="brand-logo lg">特安</span>
        <h1>特种设备全生命周期监管平台</h1>
        <p>电梯 · 起重机械 | 检验 · 维保 · 监察</p>
      </div>
      <form @submit.prevent="onSubmit">
        <label>
          用户名
          <input v-model.trim="form.username" autocomplete="username" placeholder="请输入用户名" required />
        </label>
        <label>
          密码
          <input v-model="form.password" type="password" autocomplete="current-password" placeholder="请输入密码" required />
        </label>
        <p v-if="error" class="login-error">{{ error }}</p>
        <button class="btn btn-primary btn-block" :disabled="loading">
          {{ loading ? '登录中…' : '登 录' }}
        </button>
      </form>
      <div class="login-demo">
        <p>演示账号（密码均为 <code>Tean@2026</code>）：</p>
        <div class="demo-accounts">
          <span v-for="a in demoAccounts" :key="a.u" @click="fill(a.u)">{{ a.u }}<i>{{ a.r }}</i></span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { api } from '../api/client'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const form = reactive({ username: '', password: '' })
const loading = ref(false)
const error = ref('')

const demoAccounts = [
  { u: 'eq_admin', r: '设备管理员' },
  { u: 'maint01', r: '维保人员' },
  { u: 'inspector01', r: '检验员' },
  { u: 'superv01', r: '监察员' }
]

function fill(username) {
  form.username = username
  form.password = 'Tean@2026'
}

async function onSubmit() {
  loading.value = true
  error.value = ''
  try {
    const { data } = await api.post('/auth/login', form)
    auth.setSession(data.data.token, data.data.user)
    router.push(route.query.redirect || '/')
  } catch (e) {
    error.value = e.message || '登录失败'
  } finally {
    loading.value = false
  }
}
</script>
