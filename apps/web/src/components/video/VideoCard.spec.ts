import { mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { describe, expect, it } from 'vitest'
import type { VideoCard as VideoCardModel } from '../../api/videoApi'
import VideoCard from './VideoCard.vue'

const video: VideoCardModel = {
  videoId: 'video-42',
  title: '一段足够长的视频标题，用于确认卡片会按两行截断并且保留完整链接信息',
  coverUrl: 'https://example.test/cover.jpg',
  durationSeconds: 3742,
  creator: { creatorId: 'creator-1', displayName: '创作者', avatarUrl: 'https://example.test/avatar.jpg' },
  viewCount: '12840',
  publishedAt: '2026-08-14T09:30:00Z',
  category: '生活',
  recommendationReason: '新鲜发布',
}

describe('VideoCard', () => {
  it('shows video metadata and routes to the watch page', async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/watch/:videoId', name: 'watch', component: { template: '<div />' } }],
    })
    await router.push('/watch/video-42')
    await router.isReady()
    const wrapper = mount(VideoCard, { props: { video }, global: { plugins: [router] } })

    expect(wrapper.text()).toContain('01:02:22')
    expect(wrapper.text()).toContain('1.3 万次观看')
    expect(wrapper.get('.video-card__title').attributes('href')).toBe('/watch/video-42')
    expect(wrapper.get('.video-card__reason').text()).toBe('新鲜发布')

    await wrapper.get('img').trigger('error')
    expect(wrapper.get('.video-card__image-fallback').text()).toBe('视频封面暂不可用')
  })
})
