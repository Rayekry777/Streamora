import type { components } from '../../../../packages/openapi/generated/streamora-v1'
import { ApiError, apiRequest } from './http'
import {
  getDemoHomeFeed,
  getDemoVideoDetail,
  getDemoVideoPlayback,
} from '../mocks/videoDemo'

export type HomeFeed = components['schemas']['HomeFeedView']
export type VideoCard = components['schemas']['VideoCardView']
export type VideoDetail = components['schemas']['VideoDetailView']
export type VideoPlayback = components['schemas']['VideoPlaybackView']

type HomeFeedResponse = components['schemas']['HomeFeedResponse']
type VideoDetailResponse = components['schemas']['VideoDetailResponse']
type VideoPlaybackResponse = components['schemas']['VideoPlaybackResponse']

const useRemoteVideoApi = import.meta.env.VITE_USE_REMOTE_VIDEO_API === 'true'

function canUseDemoFallback(error: unknown): boolean {
  return !useRemoteVideoApi || error instanceof ApiError && [404, 501, 503].includes(error.status) || error instanceof TypeError
}

export async function getHomeFeed(category?: string): Promise<HomeFeed> {
  if (!useRemoteVideoApi) {
    return getDemoHomeFeed(category)
  }

  try {
    const params = new URLSearchParams()
    if (category) params.set('category', category)
    const suffix = params.size > 0 ? `?${params}` : ''
    const response = await apiRequest<HomeFeedResponse>(`/api/v1/home/feed${suffix}`)
    return response.data
  } catch (error) {
    if (canUseDemoFallback(error)) return getDemoHomeFeed(category)
    throw error
  }
}

export async function getVideoDetail(videoId: string): Promise<VideoDetail> {
  if (!useRemoteVideoApi) {
    return getDemoVideoDetail(videoId)
  }

  try {
    const response = await apiRequest<VideoDetailResponse>(`/api/v1/videos/${encodeURIComponent(videoId)}`)
    return response.data
  } catch (error) {
    if (canUseDemoFallback(error)) return getDemoVideoDetail(videoId)
    throw error
  }
}

export async function getVideoPlayback(videoId: string): Promise<VideoPlayback> {
  if (!useRemoteVideoApi) {
    return getDemoVideoPlayback(videoId)
  }

  try {
    const response = await apiRequest<VideoPlaybackResponse>(`/api/v1/videos/${encodeURIComponent(videoId)}/playback`)
    return response.data
  } catch (error) {
    if (canUseDemoFallback(error)) return getDemoVideoPlayback(videoId)
    throw error
  }
}
