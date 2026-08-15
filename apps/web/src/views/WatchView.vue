<script setup lang="ts">
import { useQuery } from '@tanstack/vue-query'
import { MessageSquareText, RefreshCw } from 'lucide-vue-next'
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import { getVideoDetail, getVideoPlayback } from '../api/videoApi'
import ContentSkeleton from '../components/video/ContentSkeleton.vue'
import CreatorSummary from '../components/video/CreatorSummary.vue'
import EngagementBar from '../components/video/EngagementBar.vue'
import PlaylistPanel from '../components/video/PlaylistPanel.vue'
import VideoPlayer from '../components/video/VideoPlayer.vue'
import { formatPublishedAt } from '../utils/formatters'

const route = useRoute()
const videoId = computed(() => String(route.params.videoId))
const playerState = ref<'playing' | 'paused' | 'buffering'>('paused')
const detailQuery = useQuery({
  queryKey: ['video-detail', videoId],
  queryFn: () => getVideoDetail(videoId.value),
})
const playbackQuery = useQuery({
  queryKey: ['video-playback', videoId],
  queryFn: () => getVideoPlayback(videoId.value),
})
</script>

<template>
  <section class="watch-page">
    <ContentSkeleton v-if="detailQuery.isPending.value || playbackQuery.isPending.value" />
    <div
      v-else-if="detailQuery.isError.value || playbackQuery.isError.value"
      class="page-state"
      role="alert"
    >
      <strong>视频暂时无法打开</strong>
      <span>请稍后重试，或返回首页浏览其他内容。</span>
      <button
        type="button"
        @click="detailQuery.refetch(); playbackQuery.refetch()"
      >
        <RefreshCw :size="16" />
        重试
      </button>
    </div>
    <template v-else-if="detailQuery.data.value && playbackQuery.data.value">
      <div class="watch-layout">
        <main class="watch-main">
          <div id="streamora-player-fullscreen-host">
            <VideoPlayer
              :playback="playbackQuery.data.value"
              @playback-change="playerState = $event"
            />
          </div>
          <div class="watch-title-row">
            <div>
              <p>{{ detailQuery.data.value.category }} · {{ formatPublishedAt(detailQuery.data.value.publishedAt) }}</p>
              <h1>{{ detailQuery.data.value.title }}</h1>
            </div>
            <span class="player-state">{{ playerState === 'playing' ? '正在播放' : playerState === 'buffering' ? '正在缓冲' : '已暂停' }}</span>
          </div>
          <EngagementBar :views="detailQuery.data.value.viewCount" />
          <CreatorSummary :creator="detailQuery.data.value.creator" />
          <section class="video-description">
            <div>
              <h2>视频简介</h2>
              <p>{{ detailQuery.data.value.description }}</p>
            </div>
            <div class="tag-list">
              <span
                v-for="tag in detailQuery.data.value.tags"
                :key="tag"
              ># {{ tag }}</span>
            </div>
          </section>
          <section class="comments-placeholder">
            <MessageSquareText :size="22" />
            <div>
              <h2>评论区将在阶段 4 开放</h2>
              <p>弹幕、评论、点赞与收藏会在社区互动链路完成后接入。</p>
            </div>
          </section>
        </main>
        <PlaylistPanel
          :episodes="detailQuery.data.value.episodes"
          :recommendations="detailQuery.data.value.recommendedVideos"
        />
      </div>
    </template>
  </section>
</template>
