import { QueryClient, VueQueryPlugin } from '@tanstack/vue-query'
import { createPinia } from 'pinia'
import { createApp } from 'vue'
import '@streamora/ui-tokens/tokens.css'
import 'uno.css'
import App from './App.vue'
import router from './router'
import { useUserAuthStore } from './stores/userAuth'
import './style.css'

const app = createApp(App)
const pinia = createPinia()
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      retry: 1,
    },
  },
})

app.use(pinia)
app.use(VueQueryPlugin, { queryClient })

void useUserAuthStore(pinia).bootstrap().finally(() => {
  app.use(router)
  app.mount('#app')
})
