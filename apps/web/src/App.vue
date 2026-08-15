<script setup lang="ts">
import { storeToRefs } from 'pinia'
import { RouterLink, RouterView } from 'vue-router'
import GlobalPetHost from './pet/GlobalPetHost.vue'
import { useUserAuthStore } from './stores/userAuth'

const auth = useUserAuthStore()
const { busy, isAuthenticated, session } = storeToRefs(auth)

async function logout(): Promise<void> {
  await auth.logout()
}
</script>

<template>
  <div class="user-shell">
    <header class="topbar">
      <RouterLink
        class="brand"
        to="/"
        aria-label="Streamora 首页"
      >
        <span class="brand-mark">S</span>
        <span>Streamora</span>
      </RouterLink>

      <nav
        class="primary-nav"
        aria-label="主导航"
      >
        <RouterLink to="/">
          首页
        </RouterLink>
        <RouterLink to="/explore">
          发现
        </RouterLink>
        <RouterLink to="/upload">
          投稿
        </RouterLink>
      </nav>

      <div class="topbar-actions">
        <span
          v-if="isAuthenticated"
          class="account-label"
        >
          {{ session?.displayName }}
        </span>
        <button
          v-if="isAuthenticated"
          class="ghost-button"
          type="button"
          :disabled="busy"
          @click="logout"
        >
          退出
        </button>
        <RouterLink
          v-else
          class="ghost-button"
          to="/login"
        >
          登录
        </RouterLink>
        <RouterLink
          class="primary-button"
          to="/upload"
        >
          发布视频
        </RouterLink>
      </div>
    </header>

    <main class="page-content">
      <RouterView />
    </main>

    <GlobalPetHost />
  </div>
</template>
