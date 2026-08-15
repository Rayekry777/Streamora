<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ApiError } from '../api/http'
import { useAdminAuthStore } from '../stores/adminAuth'

const auth = useAdminAuthStore()
const route = useRoute()
const router = useRouter()
const login = ref('')
const password = ref('')
const errorMessage = ref('')

async function submit(): Promise<void> {
  errorMessage.value = ''
  try {
    await auth.login(login.value, password.value)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    await router.replace(redirect)
  } catch (error) {
    errorMessage.value = error instanceof ApiError ? error.message : '管理端暂时不可用'
  }
}
</script>

<template>
  <main class="admin-login-page">
    <section class="admin-login-card">
      <div class="admin-login-brand">
        <span>S</span>
        <div><strong>Streamora</strong><small>Operations</small></div>
      </div>
      <span class="breadcrumb">SECURE ADMIN ACCESS</span>
      <h1>运营管理端登录</h1>
      <p>管理会话与用户会话完全隔离。仅已分配管理角色的身份可以进入。</p>

      <form @submit.prevent="submit">
        <label>
          管理员登录名
          <input
            v-model.trim="login"
            autocomplete="username"
            minlength="3"
            maxlength="64"
            required
          >
        </label>
        <label>
          密码
          <input
            v-model="password"
            type="password"
            autocomplete="current-password"
            minlength="6"
            maxlength="72"
            required
          >
        </label>
        <p
          v-if="errorMessage"
          class="admin-form-error"
          role="alert"
        >
          {{ errorMessage }}
        </p>
        <button
          class="admin-primary"
          type="submit"
          :disabled="auth.busy"
        >
          {{ auth.busy ? '安全验证中…' : '进入运营工作台' }}
        </button>
      </form>
    </section>
  </main>
</template>
