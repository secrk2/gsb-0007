import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import { useOfflineStore } from './store/offline'
import './styles.css'

const app = createApp(App)
const pinia = createPinia()
app.use(pinia)
app.use(router)

// 离线/恢复监听与队列自动合并
useOfflineStore().init()

app.mount('#app')
