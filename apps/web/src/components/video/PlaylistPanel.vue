<script setup lang="ts">
import { ListVideo, Play } from 'lucide-vue-next'
import type { VideoCard } from '../../api/videoApi'
import { formatDuration } from '../../utils/formatters'

defineProps<{
  episodes: { episodeId: string; title: string; durationSeconds: number; isCurrent: boolean }[]
  recommendations: VideoCard[]
}>()
</script>

<template>
  <aside class="playlist-panel">
    <section>
      <div class="playlist-panel__heading">
        <ListVideo :size="19" />
        <h2>连续观看</h2>
      </div>
      <ol class="episode-list">
        <li
          v-for="episode in episodes"
          :key="episode.episodeId"
          :class="{ 'is-current': episode.isCurrent }"
        >
          <Play
            :size="14"
            :fill="episode.isCurrent ? 'currentColor' : 'none'"
          />
          <span>{{ episode.title }}</span>
          <time>{{ formatDuration(episode.durationSeconds) }}</time>
        </li>
      </ol>
    </section>
    <section>
      <div class="playlist-panel__heading">
        <h2>接下来推荐</h2>
      </div>
      <div class="recommendation-list">
        <RouterLink
          v-for="video in recommendations.slice(0, 4)"
          :key="video.videoId"
          :to="{ name: 'watch', params: { videoId: video.videoId } }"
        >
          <img
            :src="video.coverUrl"
            :alt="video.title"
          >
          <span>
            <strong>{{ video.title }}</strong>
            <small>{{ video.creator.displayName }}</small>
          </span>
        </RouterLink>
      </div>
    </section>
  </aside>
</template>
