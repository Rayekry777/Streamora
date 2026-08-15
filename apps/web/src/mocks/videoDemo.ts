import type { HomeFeed, VideoCard, VideoDetail, VideoPlayback } from '../api/videoApi'

const coverImages = {
  city: 'https://images.unsplash.com/photo-1477959858617-67f85cf4f1df?auto=format&fit=crop&w=1200&q=85',
  kitchen: 'https://images.unsplash.com/photo-1498837167922-ddd27525d352?auto=format&fit=crop&w=1200&q=85',
  coast: 'https://images.unsplash.com/photo-1511497584788-876760111969?auto=format&fit=crop&w=1200&q=85',
  studio: 'https://images.unsplash.com/photo-1526498460520-4c246339dccb?auto=format&fit=crop&w=1200&q=85',
  game: 'https://images.unsplash.com/photo-1511512578047-dfb367046420?auto=format&fit=crop&w=1200&q=85',
  nature: 'https://images.unsplash.com/photo-1469474968028-56623f02e42e?auto=format&fit=crop&w=1200&q=85',
} as const

const avatars = {
  studio: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=160&q=80',
  kitchen: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=160&q=80',
  coast: 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=160&q=80',
} as const

const demoVideos: VideoCard[] = [
  {
    videoId: 'city-pet-journey', title: '和宠物一起探索城市的安静角落', coverUrl: coverImages.city, durationSeconds: 584,
    creator: { creatorId: 'streamora-studio', displayName: 'Streamora Studio', avatarUrl: avatars.studio }, viewCount: '12840', publishedAt: '2026-08-14T09:30:00Z', category: '生活', recommendationReason: '因为你常看陪伴感内容',
  },
  {
    videoId: 'morning-breakfast', title: '三分钟做一份会让人慢下来的治愈早餐', coverUrl: coverImages.kitchen, durationSeconds: 206,
    creator: { creatorId: 'morning-kitchen', displayName: '清晨厨房', avatarUrl: avatars.kitchen }, viewCount: '9650', publishedAt: '2026-08-14T02:15:00Z', category: '生活', recommendationReason: '新鲜发布',
  },
  {
    videoId: 'coast-diary', title: '第一次带它去看海，风比想象中温柔', coverUrl: coverImages.coast, durationSeconds: 424,
    creator: { creatorId: 'coast-diary', displayName: '海边日记', avatarUrl: avatars.coast }, viewCount: '23890', publishedAt: '2026-08-13T11:40:00Z', category: '萌宠', recommendationReason: '热门内容',
  },
  {
    videoId: 'desk-setup', title: '一个让创作更专注的桌面整理流程', coverUrl: coverImages.studio, durationSeconds: 762,
    creator: { creatorId: 'streamora-studio', displayName: 'Streamora Studio', avatarUrl: avatars.studio }, viewCount: '7420', publishedAt: '2026-08-13T05:20:00Z', category: '知识', recommendationReason: '与你关注的创作者相关',
  },
  {
    videoId: 'indie-game-night', title: '独立游戏夜：用一小时走进一座会呼吸的小城', coverUrl: coverImages.game, durationSeconds: 3942,
    creator: { creatorId: 'pixel-signal', displayName: '像素讯号', avatarUrl: avatars.coast }, viewCount: '31560', publishedAt: '2026-08-12T15:05:00Z', category: '游戏', recommendationReason: '正在上升',
  },
  {
    videoId: 'forest-soundscape', title: '雨后森林的声音采集：一段给耳朵的散步', coverUrl: coverImages.nature, durationSeconds: 918,
    creator: { creatorId: 'field-notes', displayName: '原野笔记', avatarUrl: avatars.kitchen }, viewCount: '11820', publishedAt: '2026-08-12T07:25:00Z', category: '知识', recommendationReason: '因为你喜欢安静氛围',
  },
]

const categories = [
  { categoryId: 'all', label: '推荐', icon: 'Sparkles' },
  { categoryId: 'life', label: '生活', icon: 'Coffee' },
  { categoryId: 'pets', label: '萌宠', icon: 'Heart' },
  { categoryId: 'games', label: '游戏', icon: 'Gamepad2' },
  { categoryId: 'knowledge', label: '知识', icon: 'Lightbulb' },
  { categoryId: 'technology', label: '科技', icon: 'Cpu' },
]

const categoryLabels: Record<string, string> = {
  life: '生活', pets: '萌宠', games: '游戏', knowledge: '知识', technology: '科技', all: '推荐',
}

export function getDemoHomeFeed(category?: string): HomeFeed {
  const label = category ? categoryLabels[category] : undefined
  const items = label && label !== '推荐' ? demoVideos.filter((video) => video.category === label) : demoVideos
  return {
    featuredVideo: demoVideos[0],
    categories,
    items: items.length > 0 ? items : demoVideos,
    nextCursor: null,
    hasMore: false,
  }
}

export function getDemoVideoDetail(videoId: string): VideoDetail {
  const current = demoVideos.find((video) => video.videoId === videoId) ?? demoVideos[0]
  return {
    ...current,
    description: '这是一段用于 Streamora 封闭演示的公开视频内容。真实的视频信息、分集与推荐列表将在媒体处理、审核和发布链路完成后由领域服务提供。',
    tags: [current.category, 'AI 陪伴', '创作日常'],
    episodes: [
      { episodeId: current.videoId, title: '第 1 集 · 正片', durationSeconds: current.durationSeconds, isCurrent: true },
      { episodeId: `${current.videoId}-trailer`, title: '预告 · 开始之前', durationSeconds: 68, isCurrent: false },
    ],
    recommendedVideos: demoVideos.filter((video) => video.videoId !== current.videoId),
  }
}

export function getDemoVideoPlayback(videoId: string): VideoPlayback {
  const current = demoVideos.find((video) => video.videoId === videoId) ?? demoVideos[0]
  return {
    videoId: current.videoId,
    manifestUrl: 'https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8',
    posterUrl: current.coverUrl,
    subtitles: [],
    expiresAt: '2026-12-31T00:00:00Z',
  }
}
