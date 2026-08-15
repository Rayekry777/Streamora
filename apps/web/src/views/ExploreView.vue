<script setup lang="ts">
import { SearchX } from 'lucide-vue-next'
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const keyword = computed(() => typeof route.query.q === 'string' ? route.query.q : '')
</script>

<template>
  <section class="explore-placeholder">
    <SearchX :size="34" />
    <h1>{{ keyword ? `“${keyword}” 的搜索结果` : '探索内容分区' }}</h1>
    <p>{{ keyword ? '搜索服务将在阶段 7 接入。现在可以从首页分区继续浏览。' : '分区推荐已在首页开放；搜索和关注流将在阶段 7 接入。' }}</p>
  </section>
</template>
