<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ApiError } from '../api/http'
import { useUserAuthStore } from '../stores/userAuth'

const auth = useUserAuthStore()
const route = useRoute()
const router = useRouter()
const mode = ref<'login' | 'register'>('login')
const login = ref('')
const displayName = ref('')
const password = ref('')
const errorMessage = ref('')

async function submit(): Promise<void> {
  errorMessage.value = ''
  try {
    if (mode.value === 'register') {
      await auth.register(login.value, displayName.value, password.value)
    } else {
      await auth.login(login.value, password.value)
    }
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    await router.replace(redirect)
  } catch (error) {
    errorMessage.value = error instanceof ApiError ? error.message : '暂时无法登录，请稍后再试'
  }
}
</script>

<template>
  <section class="auth-page">
    <div class="auth-card">
      <span class="eyebrow">USER SPACE</span>
      <h1>{{ mode === 'login' ? '欢迎回来' : '创建你的 Streamora 身份' }}</h1>
      <p>登录后，公共吉祥物会在当前全局实例中切换为你的个人伙伴。</p>

      <form @submit.prevent="submit">
        <label>
          登录名
          <input
            v-model.trim="login"
            autocomplete="username"
            minlength="3"
            maxlength="64"
            required
          >
        </label>
        <label v-if="mode === 'register'">
          昵称
          <input
            v-model.trim="displayName"
            autocomplete="nickname"
            maxlength="40"
            required
          >
        </label>
        <label>
          密码
          <input
            v-model="password"
            type="password"
            :autocomplete="mode === 'login' ? 'current-password' : 'new-password'"
            minlength="12"
            maxlength="72"
            required
          >
        </label>

        <p
          v-if="errorMessage"
          class="form-error"
          role="alert"
        >
          {{ errorMessage }}
        </p>
        <button
          class="primary-button auth-submit"
          type="submit"
          :disabled="auth.busy"
        >
          {{ auth.busy ? '处理中…' : mode === 'login' ? '登录' : '注册并登录' }}
        </button>
      </form>

      <button
        class="auth-mode-button"
        type="button"
        @click="mode = mode === 'login' ? 'register' : 'login'"
      >
        {{ mode === 'login' ? '还没有账号？立即注册' : '已有账号？返回登录' }}
      </button>
    </div>
  </section>
</template>
