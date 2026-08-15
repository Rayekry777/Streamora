<script setup lang="ts">
import type { VideoCard as VideoCardModel } from '../../api/videoApi'
import VideoCard from './VideoCard.vue'

defineProps<{
  videos: VideoCardModel[]
}>()
</script>

<template>
  <div class="video-grid">
    <VideoCard
      v-for="video in videos"
      :key="video.videoId"
      :video="video"
    />
  </div>
</template>
