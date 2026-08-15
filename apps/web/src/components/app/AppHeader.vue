<script setup lang="ts">
import { Bell, ChevronDown, Compass, House, Menu, MessageCircle, Search, Upload, UserRound } from 'lucide-vue-next'
import { storeToRefs } from 'pinia'
import { ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import SearchBox from './SearchBox.vue'
import { useUserAuthStore } from '../../stores/userAuth'

const router = useRouter()
const auth = useUserAuthStore()
const { busy, isAuthenticated, session } = storeToRefs(auth)
const menuOpen = ref(false)
const mobileSearchOpen = ref(false)

function search(keyword: string): void {
  void router.push({ name: 'explore', query: { q: keyword } })
  mobileSearchOpen.value = false
}

function syncAccountMenu(event: Event): void {
  menuOpen.value = (event.currentTarget as HTMLDetailsElement).open
}

async function logout(): Promise<void> {
  menuOpen.value = false
  await auth.logout()
  await router.push({ name: 'home' })
}
</script>

<template>
  <header class="app-header">
    <div class="app-header__inner">
      <div class="app-header__leading">
        <RouterLink
          class="brand"
          to="/"
          aria-label="Streamora 首页"
        >
          <span class="brand-mark">S</span>
          <span class="brand-name">Streamora</span>
        </RouterLink>
        <nav
          class="desktop-nav"
          aria-label="主导航"
        >
          <RouterLink to="/">
            <House :size="17" />
            首页
          </RouterLink>
          <RouterLink to="/explore">
            <Compass :size="17" />
            分区
          </RouterLink>
        </nav>
      </div>

      <SearchBox
        class="app-header__search"
        @submit="search"
      />

      <div class="app-header__actions">
        <button
          class="icon-button mobile-search-trigger"
          type="button"
          aria-label="打开搜索"
          title="搜索"
          @click="mobileSearchOpen = !mobileSearchOpen"
        >
          <Search :size="20" />
        </button>
        <button
          class="icon-button desktop-action"
          type="button"
          aria-label="消息中心"
          title="消息中心"
        >
          <MessageCircle :size="19" />
        </button>
        <button
          class="icon-button desktop-action"
          type="button"
          aria-label="通知"
          title="通知"
        >
          <Bell :size="19" />
        </button>
        <RouterLink
          class="publish-button"
          to="/upload"
        >
          <Upload :size="17" />
          投稿
        </RouterLink>

        <details
          v-if="isAuthenticated"
          class="account-menu"
          :open="menuOpen"
          @toggle="syncAccountMenu"
        >
          <summary aria-label="打开用户菜单">
            <span class="avatar avatar--initial">{{ session?.displayName.slice(0, 1) }}</span>
            <ChevronDown :size="15" />
          </summary>
          <div class="account-menu__panel">
            <strong>{{ session?.displayName }}</strong>
            <span>个人中心将在后续阶段开放</span>
            <button
              type="button"
              :disabled="busy"
              @click="logout"
            >
              退出登录
            </button>
          </div>
        </details>
        <RouterLink
          v-else
          class="login-link"
          to="/login"
        >
          <UserRound :size="18" />
          登录
        </RouterLink>
      </div>
    </div>
    <div
      v-if="mobileSearchOpen"
      class="mobile-search-panel"
    >
      <SearchBox
        compact
        @submit="search"
      />
    </div>
    <nav
      class="mobile-nav"
      aria-label="移动端主导航"
    >
      <RouterLink to="/">
        <House :size="18" />
        首页
      </RouterLink>
      <RouterLink to="/explore">
        <Compass :size="18" />
        分区
      </RouterLink>
      <RouterLink to="/upload">
        <Menu :size="18" />
        创作
      </RouterLink>
    </nav>
  </header>
</template>
