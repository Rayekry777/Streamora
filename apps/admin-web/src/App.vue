<script setup lang="ts">
import { RouterLink, RouterView } from 'vue-router'

const navigation = [
  { to: '/', label: '运营概览', icon: '◫' },
  { to: '/content', label: '内容审核', icon: '▶' },
  { to: '/users', label: '用户治理', icon: '◎' },
  { to: '/media', label: '媒体任务', icon: '⇄' },
  { to: '/pets', label: '宠物资产', icon: '◇' },
  { to: '/ai', label: 'AI 运营', icon: '✦' },
  { to: '/permissions', label: '角色权限', icon: '⌘' },
  { to: '/system', label: '系统健康', icon: '●' },
]
</script>

<template>
  <div
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
          v-for="item in navigation"
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
          <strong>Foundation</strong>
          <small>阶段 1 · 本地环境</small>
        </div>
      </div>
    </aside>

    <div class="admin-main">
      <header class="admin-topbar">
        <div>
          <span class="breadcrumb">STREAMORA / ADMIN</span>
          <strong>运营工作台</strong>
        </div>
        <div class="operator-chip">
          <span>SA</span>
          <div>
            <strong>演示管理员</strong>
            <small>SUPER_ADMIN</small>
          </div>
        </div>
      </header>

      <main class="admin-content">
        <RouterView />
      </main>
    </div>
  </div>
</template>

