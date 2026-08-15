import { createRouter, createWebHistory } from 'vue-router'
import ExploreView from '../views/ExploreView.vue'
import HomeView from '../views/HomeView.vue'
import LoginView from '../views/LoginView.vue'
import NotFoundView from '../views/NotFoundView.vue'
import { useUserAuthStore } from '../stores/userAuth'
import UploadView from '../views/UploadView.vue'
import WatchView from '../views/WatchView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'home', component: HomeView, meta: { title: '首页' } },
    { path: '/explore', name: 'explore', component: ExploreView, meta: { title: '发现' } },
    { path: '/login', name: 'login', component: LoginView, meta: { title: '登录' } },
    { path: '/upload', name: 'upload', component: UploadView, meta: { title: '投稿', requiresAuth: true } },
    { path: '/watch/:videoId', name: 'watch', component: WatchView, meta: { title: '观看' } },
    { path: '/:pathMatch(.*)*', name: 'not-found', component: NotFoundView, meta: { title: '页面不存在' } },
  ],
  scrollBehavior: () => ({ top: 0 }),
})

router.beforeEach((route) => {
  const auth = useUserAuthStore()
  if (route.meta.requiresAuth && !auth.isAuthenticated) {
    return { name: 'login', query: { redirect: route.fullPath } }
  }
  if (route.name === 'login' && auth.isAuthenticated) {
    return { name: 'home' }
  }
  return true
})

router.afterEach((route) => {
  document.title = `${String(route.meta.title ?? 'Streamora')} · Streamora`
})

export default router
