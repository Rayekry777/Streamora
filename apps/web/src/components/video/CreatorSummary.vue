<script setup lang="ts">
import { BellPlus, UserPlus } from 'lucide-vue-next'

defineProps<{
  creator: { displayName: string; avatarUrl: string }
}>()
</script>

<template>
  <section class="creator-summary">
    <img
      :src="creator.avatarUrl"
      :alt="`${creator.displayName} 的头像`"
    >
    <div>
      <h2>{{ creator.displayName }}</h2>
      <p>创作中 · 关注功能将在后续阶段开放</p>
    </div>
    <div class="creator-summary__actions">
      <button
        type="button"
        disabled
        title="关注功能即将开放"
      >
        <UserPlus :size="17" />
        关注
      </button>
      <button
        type="button"
        disabled
        aria-label="订阅更新"
        title="订阅功能即将开放"
      >
        <BellPlus :size="18" />
      </button>
    </div>
  </section>
</template>
