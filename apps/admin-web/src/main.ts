import { QueryClient, VueQueryPlugin } from '@tanstack/vue-query'
import { createPinia } from 'pinia'
import { createApp } from 'vue'
import '@streamora/ui-tokens/tokens.css'
import 'uno.css'
import App from './App.vue'
import router from './router'
import './style.css'

const app = createApp(App)
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 15_000,
      retry: 1,
    },
  },
})

app.use(createPinia())
app.use(router)
app.use(VueQueryPlugin, { queryClient })
app.mount('#app')

