<script setup lang="ts">
import { Coffee, Cpu, Gamepad2, Heart, Lightbulb, Sparkles } from 'lucide-vue-next'
import type { components } from '../../../../../packages/openapi/generated/streamora-v1'

type VideoCategory = components['schemas']['VideoCategoryView']

defineProps<{
  categories: VideoCategory[]
  activeCategory?: string
}>()

defineEmits<{
  select: [categoryId: string]
}>()

const icons = { Sparkles, Coffee, Heart, Gamepad2, Lightbulb, Cpu }
</script>

<template>
  <nav
    class="category-nav"
    aria-label="视频分区"
  >
    <button
      v-for="category in categories"
      :key="category.categoryId"
      type="button"
      :class="{ 'is-active': activeCategory === category.categoryId || (!activeCategory && category.categoryId === 'all') }"
      @click="$emit('select', category.categoryId)"
    >
      <component
        :is="icons[category.icon as keyof typeof icons] ?? Sparkles"
        :size="18"
      />
      {{ category.label }}
    </button>
  </nav>
</template>
