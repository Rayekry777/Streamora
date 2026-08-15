import { createPinia, setActivePinia } from 'pinia'
import { afterEach, describe, expect, it, vi } from 'vitest'
import router from './index'
import { useAdminAuthStore } from '../stores/adminAuth'

describe('administrator route permissions', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('redirects a signed-in operator away from a module without permission', async () => {
    setActivePinia(createPinia())
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => ({
        data: {
          subjectId: 'operator-1',
          displayName: '只读运营员',
          audience: 'ADMIN',
          expiresAt: '2026-08-16T00:00:00Z',
          csrfToken: 'csrf',
          roles: ['USER_OPERATOR'],
          permissions: ['DASHBOARD_VIEW'],
        },
        requestId: 'test-request',
      }),
    }))
    await useAdminAuthStore().login('operator', 'Correct-Horse-42')

    await router.push('/content')

    expect(router.currentRoute.value.name).toBe('permission-denied')
  })
})
