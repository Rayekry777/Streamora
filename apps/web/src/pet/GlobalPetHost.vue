<script setup lang="ts">
import { ref } from 'vue'

const minimized = ref(false)

function toggleMinimized(): void {
  minimized.value = !minimized.value
}
</script>

<template>
  <aside
    class="global-pet-host"
    :class="{ 'global-pet-host--minimized': minimized }"
    data-testid="global-pet-host"
    aria-label="Streamora 全局宠物"
  >
    <div
      class="pet-bubble"
      role="status"
    >
      {{ minimized ? '我在这里' : '一起看看今天的新视频吧！' }}
    </div>
    <button
      class="pet-placeholder"
      type="button"
      aria-label="缩小或展开宠物"
      @click="toggleMinimized"
    >
      <span class="pet-ear pet-ear--left" />
      <span class="pet-ear pet-ear--right" />
      <span class="pet-face">•ᴗ•</span>
    </button>
  </aside>
</template>

