import { QueryClient, VueQueryPlugin } from '@tanstack/vue-query'
import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { describe, expect, it, vi } from 'vitest'
import { getDemoHomeFeed } from '../mocks/videoDemo'

const mocks = vi.hoisted(() => ({ getHomeFeed: vi.fn() }))

vi.mock('../api/videoApi', () => ({ getHomeFeed: mocks.getHomeFeed }))

import HomeView from './HomeView.vue'

describe('HomeView', () => {
  it('renders a content-first feed and switches category through the route', async () => {
    mocks.getHomeFeed.mockImplementation((category?: string) => Promise.resolve(getDemoHomeFeed(category)))
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/', name: 'home', component: HomeView },
        { path: '/explore', name: 'explore', component: { template: '<div />' } },
        { path: '/watch/:videoId', name: 'watch', component: { template: '<div />' } },
      ],
    })
    await router.push('/')
    await router.isReady()
    const wrapper = mount(HomeView, {
      global: {
        plugins: [router, [VueQueryPlugin, { queryClient: new QueryClient({ defaultOptions: { queries: { retry: false } } }) }]],
      },
    })
    await flushPromises()

    expect(wrapper.get('.featured-video').text()).toContain('和宠物一起探索城市')
    expect(wrapper.findAll('.video-card')).toHaveLength(6)

    await wrapper.get('.category-nav button:nth-child(2)').trigger('click')
    await vi.waitFor(() => {
      expect(router.currentRoute.value.query.category).toBe('life')
    })
    expect(mocks.getHomeFeed).toHaveBeenCalledWith('life')
  })
})
