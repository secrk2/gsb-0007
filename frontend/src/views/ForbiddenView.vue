<template>
  <div class="card error-state">
    <div class="code">403</div>
    <div class="msg">无权访问：该资源属于其他单位，或当前角色无此权限</div>
    <button class="btn" @click="$router.push('/')">返回作战台</button>
  </div>
</template>
