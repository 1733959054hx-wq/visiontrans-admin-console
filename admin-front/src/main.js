import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
import { useAuthStore } from './stores/auth'

import '@fortawesome/fontawesome-free/css/all.min.css'
import './styles/index.css'

const app = createApp(App)

app.use(createPinia())
app.use(router)

// 启动时用本地令牌恢复登录态（失败则清理并回到登录页）
const auth = useAuthStore()
auth.restore().finally(() => {
  app.mount('#app')
})
