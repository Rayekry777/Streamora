<script setup lang="ts">
import { Bookmark, Heart, Share2 } from 'lucide-vue-next'

defineProps<{
  views: string
}>()
</script>

<template>
  <div class="engagement-bar">
    <span>{{ views }} 次观看</span>
    <div>
      <button
        type="button"
        disabled
        title="互动功能将在后续阶段开放"
      >
        <Heart :size="18" />
        点赞
      </button>
      <button
        type="button"
        disabled
        title="互动功能将在后续阶段开放"
      >
        <Bookmark :size="18" />
        收藏
      </button>
      <button
        type="button"
        disabled
        title="分享功能将在后续阶段开放"
      >
        <Share2 :size="18" />
        分享
      </button>
    </div>
  </div>
</template>
