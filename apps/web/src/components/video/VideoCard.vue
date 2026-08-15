<script setup lang="ts">
import { Play } from 'lucide-vue-next'
import { ref } from 'vue'
import { RouterLink } from 'vue-router'
import type { VideoCard } from '../../api/videoApi'
import { formatDuration, formatPublishedAt, formatViews } from '../../utils/formatters'

defineProps<{
  video: VideoCard
  compact?: boolean
}>()

const imageFailed = ref(false)
</script>

<template>
  <article
    class="video-card"
    :class="{ 'video-card--compact': compact }"
  >
    <RouterLink
      class="video-card__cover"
      :to="{ name: 'watch', params: { videoId: video.videoId } }"
      :aria-label="`播放：${video.title}`"
    >
      <img
        v-if="!imageFailed"
        :src="video.coverUrl"
        :alt="video.title"
        loading="lazy"
        @error="imageFailed = true"
      >
      <span
        v-else
        class="video-card__image-fallback"
      >视频封面暂不可用</span>
      <span class="video-card__duration">{{ formatDuration(video.durationSeconds) }}</span>
      <span class="video-card__play"><Play
        :size="18"
        fill="currentColor"
      /></span>
    </RouterLink>
    <div class="video-card__body">
      <RouterLink
        class="video-card__title"
        :to="{ name: 'watch', params: { videoId: video.videoId } }"
      >
        {{ video.title }}
      </RouterLink>
      <p class="video-card__meta">
        {{ video.creator.displayName }}
      </p>
      <p class="video-card__meta">
        {{ formatViews(video.viewCount) }}次观看 · {{ formatPublishedAt(video.publishedAt) }}
      </p>
      <span
        v-if="video.recommendationReason && !compact"
        class="video-card__reason"
      >{{ video.recommendationReason }}</span>
    </div>
  </article>
</template>
