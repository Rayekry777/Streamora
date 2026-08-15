<script setup lang="ts">
import { useQuery } from '@tanstack/vue-query'
import { ArrowRight, Play, RefreshCw } from 'lucide-vue-next'
import { computed, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { getHomeFeed } from '../api/videoApi'
import CategoryNav from '../components/video/CategoryNav.vue'
import ContentSkeleton from '../components/video/ContentSkeleton.vue'
import VideoGrid from '../components/video/VideoGrid.vue'
import { formatDuration, formatViews } from '../utils/formatters'

const route = useRoute()
const router = useRouter()
const activeCategory = computed(() => typeof route.query.category === 'string' ? route.query.category : 'all')
const featuredImageFailed = ref(false)
const feedQuery = useQuery({
  queryKey: ['home-feed', activeCategory],
  queryFn: () => getHomeFeed(activeCategory.value),
})

function selectCategory(categoryId: string): void {
  void router.push({ name: 'home', query: categoryId === 'all' ? {} : { category: categoryId } })
}
</script>

<template>
  <section class="home-page">
    <CategoryNav
      v-if="feedQuery.data.value"
      :categories="feedQuery.data.value.categories"
      :active-category="activeCategory"
      @select="selectCategory"
    />

    <ContentSkeleton v-if="feedQuery.isPending.value" />

    <div
      v-else-if="feedQuery.isError.value"
      class="page-state"
      role="alert"
    >
      <strong>内容暂时无法加载</strong>
      <span>请检查网络后重试。</span>
      <button
        type="button"
        @click="feedQuery.refetch()"
      >
        <RefreshCw :size="16" />
        重试
      </button>
    </div>

    <template v-else-if="feedQuery.data.value">
      <RouterLink
        class="featured-video"
        :to="{ name: 'watch', params: { videoId: feedQuery.data.value.featuredVideo.videoId } }"
      >
        <img
          v-if="!featuredImageFailed"
          :src="feedQuery.data.value.featuredVideo.coverUrl"
          :alt="feedQuery.data.value.featuredVideo.title"
          @error="featuredImageFailed = true"
        >
        <div
          v-else
          class="featured-video__image-fallback"
        />
        <div class="featured-video__overlay">
          <span>今日焦点</span>
          <h1>{{ feedQuery.data.value.featuredVideo.title }}</h1>
          <p>{{ feedQuery.data.value.featuredVideo.creator.displayName }} · {{ formatViews(feedQuery.data.value.featuredVideo.viewCount) }}次观看</p>
          <strong><Play
            :size="17"
            fill="currentColor"
          /> {{ formatDuration(feedQuery.data.value.featuredVideo.durationSeconds) }}</strong>
        </div>
      </RouterLink>

      <section class="content-section">
        <div class="section-heading">
          <div>
            <p>为你精选</p>
            <h2>{{ activeCategory === 'all' ? '正在发生的好内容' : '这个分区正在更新' }}</h2>
          </div>
          <RouterLink to="/explore">
            查看分区
            <ArrowRight :size="17" />
          </RouterLink>
        </div>
        <VideoGrid :videos="feedQuery.data.value.items" />
      </section>

      <div
        v-if="feedQuery.data.value.items.length === 0"
        class="page-state"
      >
        <strong>这个分区还没有公开视频</strong>
        <span>换一个分区看看，或者稍后再来。</span>
      </div>
    </template>
  </section>
</template>
