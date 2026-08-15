<script setup lang="ts">
import { computed } from 'vue'
import { storeToRefs } from 'pinia'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import { useAdminAuthStore } from './stores/adminAuth'

const navigation = [
  { to: '/', label: '运营概览', icon: '◫', permission: 'DASHBOARD_VIEW' },
  { to: '/content', label: '内容审核', icon: '▶', permission: 'CONTENT_MODERATE' },
  { to: '/users', label: '用户治理', icon: '◎', permission: 'USER_GOVERN' },
  { to: '/media', label: '媒体任务', icon: '⇄', permission: 'SYSTEM_OPERATE' },
  { to: '/pets', label: '宠物资产', icon: '◇', permission: 'AI_OPERATE' },
  { to: '/ai', label: 'AI 运营', icon: '✦', permission: 'AI_OPERATE' },
  { to: '/permissions', label: '角色权限', icon: '⌘', permission: 'RBAC_MANAGE' },
  { to: '/system', label: '系统健康', icon: '●', permission: 'SYSTEM_OPERATE' },
]

const auth = useAdminAuthStore()
const route = useRoute()
const router = useRouter()
const { busy, session } = storeToRefs(auth)
const publicPage = computed(() => route.meta.publicPage === true)
const permittedNavigation = computed(() => navigation.filter((item) => auth.hasPermission(item.permission)))
const primaryRole = computed(() => session.value?.roles[0] ?? 'NO_ROLE')

async function logout(): Promise<void> {
  await auth.logout()
  await router.replace({ name: 'login' })
}
</script>

<template>
  <RouterView v-if="publicPage" />
  <div
    v-else
    class="admin-shell"
    data-testid="admin-shell"
  >
    <aside class="sidebar">
      <RouterLink
        class="admin-brand"
        to="/"
      >
        <span>S</span>
        <div>
          <strong>Streamora</strong>
          <small>Operations</small>
        </div>
      </RouterLink>

      <nav aria-label="管理端导航">
        <RouterLink
          v-for="item in permittedNavigation"
          :key="item.to"
          :to="item.to"
        >
          <span class="nav-icon">{{ item.icon }}</span>
          {{ item.label }}
        </RouterLink>
      </nav>

      <div class="sidebar-footer">
        <span class="status-dot" />
        <div>
          <strong>Identity &amp; RBAC</strong>
          <small>阶段 2 · 进行中</small>
        </div>
      </div>
    </aside>

    <div class="admin-main">
      <header class="admin-topbar">
        <div>
          <span class="breadcrumb">STREAMORA / ADMIN</span>
          <strong>运营工作台</strong>
        </div>
        <div class="operator-actions">
          <div class="operator-chip">
            <span>SA</span>
            <div>
              <strong>{{ session?.displayName }}</strong>
              <small>{{ primaryRole }}</small>
            </div>
          </div>
          <button
            class="operator-logout"
            type="button"
            :disabled="busy"
            @click="logout"
          >
            退出
          </button>
        </div>
      </header>

      <main class="admin-content">
        <RouterView />
      </main>
    </div>
  </div>
</template>
