import { mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { describe, expect, it } from 'vitest'
import App from './App.vue'

describe('user application shell', () => {
  it('keeps the global pet host outside the routed page', async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/', component: { template: '<div data-testid="route-page">首页</div>' } }],
    })
    await router.push('/')
    await router.isReady()

    const wrapper = mount(App, { global: { plugins: [router] } })

    expect(wrapper.get('[data-testid="route-page"]').text()).toContain('首页')
    expect(wrapper.get('[data-testid="global-pet-host"]').attributes('data-testid')).toBe(
      'global-pet-host',
    )
  })
})
