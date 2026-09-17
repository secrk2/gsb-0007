<template>
  <div class="error-state">
    <div class="error-code">{{ code }}</div>
    <div class="error-title">{{ title }}</div>
    <p class="error-message">{{ message }}</p>
    <div class="error-actions">
      <button class="btn" @click="$router.back()">返回上一页</button>
      <router-link class="btn btn-ghost" to="/">回到作战台</router-link>
    </div>
  </div>
</template>

<script setup>
defineProps({
  code: { type: [String, Number], default: '403' },
  title: { type: String, default: '无权访问' },
  message: { type: String, default: '您没有权限查看该资源，本次访问已被拦截。' }
})
</script>
