import { mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { describe, expect, it } from 'vitest'
import App from './App.vue'

describe('admin application shell', () => {
  it('renders an isolated operations shell without the user pet host', async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/', component: { template: '<div>运营概览</div>' } }],
    })
    await router.push('/')
    await router.isReady()

    const wrapper = mount(App, { global: { plugins: [router] } })

    expect(wrapper.get('[data-testid="admin-shell"]').attributes('data-testid')).toBe('admin-shell')
    expect(wrapper.find('[data-testid="global-pet-host"]').exists()).toBe(false)
  })
})
