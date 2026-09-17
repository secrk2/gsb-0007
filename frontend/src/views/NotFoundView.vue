<template>
  <div class="card error-state" style="margin-top:40px">
    <div class="code">404</div>
    <div class="msg">页面不存在</div>
    <button class="btn" @click="$router.push('/')">返回首页</button>
  </div>
</template>
