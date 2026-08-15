<script setup lang="ts">
import { RouterLink, RouterView } from 'vue-router'
import GlobalPetHost from './pet/GlobalPetHost.vue'
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
        <button
          class="ghost-button"
          type="button"
        >
          登录
        </button>
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

