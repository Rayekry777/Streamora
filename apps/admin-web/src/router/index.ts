import { createRouter, createWebHistory } from 'vue-router'
import DashboardView from '../views/DashboardView.vue'
import LoginView from '../views/LoginView.vue'
import ModuleView from '../views/ModuleView.vue'
import NotFoundView from '../views/NotFoundView.vue'
import PermissionDeniedView from '../views/PermissionDeniedView.vue'
import { useAdminAuthStore } from '../stores/adminAuth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: LoginView, meta: { title: '管理员登录', publicPage: true } },
    { path: '/denied', name: 'permission-denied', component: PermissionDeniedView, meta: { title: '权限不足', requiresAuth: true } },
    { path: '/', name: 'dashboard', component: DashboardView, meta: { title: '运营概览', requiresAuth: true, permission: 'DASHBOARD_VIEW' } },
    { path: '/content', component: ModuleView, props: { title: '内容审核', description: '视频、评论、弹幕与举报案件。' }, meta: { title: '内容审核', requiresAuth: true, permission: 'CONTENT_MODERATE' } },
    { path: '/users', component: ModuleView, props: { title: '用户治理', description: '用户状态、处罚记录与风险操作。' }, meta: { title: '用户治理', requiresAuth: true, permission: 'USER_GOVERN' } },
    { path: '/media', component: ModuleView, props: { title: '媒体任务', description: '上传、转码、字幕和语义分析任务。' }, meta: { title: '媒体任务', requiresAuth: true, permission: 'SYSTEM_OPERATE' } },
    { path: '/pets', component: ModuleView, props: { title: '宠物资产', description: 'Live2D 模型、动作、表情与版本发布。' }, meta: { title: '宠物资产', requiresAuth: true, permission: 'AI_OPERATE' } },
    { path: '/ai', component: ModuleView, props: { title: 'AI 运营', description: '提示词、模型路由、用量与成本。' }, meta: { title: 'AI 运营', requiresAuth: true, permission: 'AI_OPERATE' } },
    { path: '/permissions', component: ModuleView, props: { title: '角色权限', description: 'RBAC、管理员角色与不可变审计。' }, meta: { title: '角色权限', requiresAuth: true, permission: 'RBAC_MANAGE' } },
    { path: '/system', component: ModuleView, props: { title: '系统健康', description: '服务、队列、数据库和可观测性状态。' }, meta: { title: '系统健康', requiresAuth: true, permission: 'SYSTEM_OPERATE' } },
    { path: '/:pathMatch(.*)*', component: NotFoundView, meta: { title: '页面不存在' } },
  ],
})

router.beforeEach((route) => {
  const auth = useAdminAuthStore()
  if (route.meta.requiresAuth && !auth.isAuthenticated) {
    return { name: 'login', query: { redirect: route.fullPath } }
  }
  const permission = typeof route.meta.permission === 'string' ? route.meta.permission : undefined
  if (permission && !auth.hasPermission(permission)) {
    return { name: 'permission-denied' }
  }
  if (route.name === 'login' && auth.isAuthenticated) {
    return { name: 'dashboard' }
  }
  return true
})

router.afterEach((route) => {
  document.title = `${String(route.meta.title ?? '管理端')} · Streamora 管理端`
})

export default router
