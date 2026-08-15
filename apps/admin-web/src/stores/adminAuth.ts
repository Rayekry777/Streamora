import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { getAdminSession, loginAdmin, logoutAdmin, type AdminSession } from '../api/adminAuthApi'
import { ApiError } from '../api/http'

export const useAdminAuthStore = defineStore('admin-auth', () => {
  const session = ref<AdminSession | null>(null)
  const initialized = ref(false)
  const busy = ref(false)
  const isAuthenticated = computed(() => session.value?.audience === 'ADMIN')

  function hasPermission(permission: string): boolean {
    return session.value?.permissions.includes(permission) ?? false
  }

  async function bootstrap(): Promise<void> {
    try {
      session.value = await getAdminSession()
    } catch (error) {
      if (!(error instanceof ApiError) || ![401, 403].includes(error.status)) {
        console.warn('管理员会话初始化失败', error)
      }
      session.value = null
    } finally {
      initialized.value = true
    }
  }

  async function login(login: string, password: string): Promise<void> {
    busy.value = true
    try {
      session.value = await loginAdmin({ login, password })
    } finally {
      busy.value = false
    }
  }

  async function logout(): Promise<void> {
    const csrfToken = session.value?.csrfToken
    if (!csrfToken) {
      session.value = null
      return
    }
    busy.value = true
    try {
      await logoutAdmin(csrfToken)
      session.value = null
    } finally {
      busy.value = false
    }
  }

  return { session, initialized, busy, isAuthenticated, hasPermission, bootstrap, login, logout }
})
