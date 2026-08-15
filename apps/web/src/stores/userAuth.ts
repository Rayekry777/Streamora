import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { ApiError } from '../api/http'
import {
  getUserSession,
  loginUser,
  logoutUser,
  registerUser,
  type AuthSession,
} from '../api/userAuthApi'

export const useUserAuthStore = defineStore('user-auth', () => {
  const session = ref<AuthSession | null>(null)
  const initialized = ref(false)
  const busy = ref(false)
  const isAuthenticated = computed(() => session.value?.audience === 'USER')
  const subjectKey = computed(() => session.value?.subjectId ?? 'anonymous')

  async function bootstrap(): Promise<void> {
    try {
      session.value = await getUserSession()
    } catch (error) {
      if (!(error instanceof ApiError) || error.status !== 401) {
        console.warn('用户会话初始化失败', error)
      }
      session.value = null
    } finally {
      initialized.value = true
    }
  }

  async function login(login: string, password: string): Promise<void> {
    busy.value = true
    try {
      session.value = await loginUser({ login, password })
    } finally {
      busy.value = false
    }
  }

  async function register(login: string, displayName: string, password: string): Promise<void> {
    busy.value = true
    try {
      session.value = await registerUser({ login, displayName, password })
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
      await logoutUser(csrfToken)
      session.value = null
    } finally {
      busy.value = false
    }
  }

  return {
    session,
    initialized,
    busy,
    isAuthenticated,
    subjectKey,
    bootstrap,
    login,
    register,
    logout,
  }
})
