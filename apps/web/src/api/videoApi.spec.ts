import { afterEach, describe, expect, it, vi } from 'vitest'

const remoteFeed = {
  featuredVideo: {
    videoId: 'remote-video', title: '来自真实接口的视频', coverUrl: 'https://example.test/cover.jpg', durationSeconds: 42,
    creator: { creatorId: 'creator-1', displayName: '创作者', avatarUrl: 'https://example.test/avatar.jpg' },
    viewCount: '1', publishedAt: '2026-08-16T00:00:00Z', category: '生活', recommendationReason: '新鲜发布',
  },
  categories: [{ categoryId: 'all', label: '推荐', icon: 'Sparkles' }],
  items: [], nextCursor: null, hasMore: false,
}

describe('videoApi remote integration mode', () => {
  afterEach(() => {
    vi.unstubAllEnvs()
    vi.unstubAllGlobals()
    vi.resetModules()
  })

  it('uses the backend feed response without falling back to demo data', async () => {
    vi.stubEnv('VITE_USE_REMOTE_VIDEO_API', 'true')
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => ({ data: remoteFeed, requestId: 'remote-read' }),
    })
    vi.stubGlobal('fetch', fetchMock)
    const { getHomeFeed } = await import('./videoApi')

    await expect(getHomeFeed('life')).resolves.toEqual(remoteFeed)
    expect(fetchMock).toHaveBeenCalledWith('/api/v1/home/feed?category=life', expect.objectContaining({ credentials: 'include' }))
  })

  it('surfaces a remote playback failure instead of rendering stale demo content', async () => {
    vi.stubEnv('VITE_USE_REMOTE_VIDEO_API', 'true')
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: false,
      status: 503,
      json: async () => ({ error: { code: 'PLAYBACK_UNAVAILABLE', message: '播放服务暂不可用' } }),
    }))
    const { getVideoPlayback } = await import('./videoApi')

    await expect(getVideoPlayback('city-pet-journey')).rejects.toMatchObject({
      status: 503,
      code: 'PLAYBACK_UNAVAILABLE',
    })
  })
})
