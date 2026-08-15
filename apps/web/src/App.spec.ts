import { QueryClient, VueQueryPlugin } from '@tanstack/vue-query'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import { afterEach, describe, expect, it, vi } from 'vitest'
import App from './App.vue'
import { useUserAuthStore } from './stores/userAuth'

describe('user application shell', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('keeps the global pet host outside the routed page', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => ({
        data: {
          petId: 'public-mascot',
          displayName: 'Streamora 小星',
          assetKey: 'placeholder/public-mascot',
          source: 'PUBLIC',
          ownerSubjectId: null,
        },
        requestId: 'test-request',
      }),
    }))
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/', component: { template: '<div data-testid="route-page">首页</div>' } }],
    })
    await router.push('/')
    await router.isReady()

    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
    const wrapper = mount(App, {
      global: { plugins: [createPinia(), router, [VueQueryPlugin, { queryClient }]] },
    })

    expect(wrapper.get('[data-testid="route-page"]').text()).toContain('首页')
    expect(wrapper.get('[data-testid="global-pet-host"]').attributes('data-testid')).toBe(
      'global-pet-host',
    )
    expect(wrapper.get('[data-testid="global-pet-host"]').attributes('data-pet-source')).toBe(
      'PUBLIC',
    )
  })

  it('switches the same global pet host from public to personal after login', async () => {
    let authenticated = false
    vi.stubGlobal('fetch', vi.fn(async (input: RequestInfo | URL) => {
      const path = String(input)
      if (path.endsWith('/api/v1/auth/login')) {
        authenticated = true
        return {
          ok: true,
          status: 200,
          json: async () => ({
            data: {
              subjectId: '42',
              displayName: '小河',
              audience: 'USER',
              expiresAt: '2026-08-16T00:00:00Z',
              csrfToken: 'csrf',
            },
            requestId: 'login-request',
          }),
        }
      }
      return {
        ok: true,
        status: 200,
        json: async () => ({
          data: authenticated
            ? {
                petId: 'personal-42',
                displayName: '小河的伙伴',
                assetKey: 'placeholder/personal-pet',
                source: 'PERSONAL',
                ownerSubjectId: '42',
              }
            : {
                petId: 'public-mascot',
                displayName: 'Streamora 小星',
                assetKey: 'placeholder/public-mascot',
                source: 'PUBLIC',
                ownerSubjectId: null,
              },
          requestId: 'pet-request',
        }),
      }
    }))
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/', component: { template: '<div>首页</div>' } }],
    })
    await router.push('/')
    await router.isReady()
    const pinia = createPinia()
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
    const wrapper = mount(App, {
      global: { plugins: [pinia, router, [VueQueryPlugin, { queryClient }]] },
    })
    await flushPromises()
    const originalHost = wrapper.get('[data-testid="global-pet-host"]').element
    expect(wrapper.get('[data-testid="global-pet-host"]').attributes('data-pet-source')).toBe('PUBLIC')

    await useUserAuthStore(pinia).login('viewer', 'Correct-Horse-42')
    await vi.waitFor(() => {
      expect(wrapper.get('[data-testid="global-pet-host"]').attributes('data-pet-source')).toBe(
        'PERSONAL',
      )
    })
    expect(wrapper.get('[data-testid="global-pet-host"]').element).toBe(originalHost)
  })
})
