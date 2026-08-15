<script setup lang="ts">
import { computed, ref } from 'vue'
import { useActivePet } from './useActivePet'

const minimized = ref(false)
const activePetQuery = useActivePet()
const pet = computed(() => activePetQuery.data.value ?? {
  petId: 'public-mascot',
  displayName: 'Streamora 小星',
  assetKey: 'placeholder/public-mascot',
  source: 'PUBLIC' as const,
  ownerSubjectId: null,
})

function toggleMinimized(): void {
  minimized.value = !minimized.value
}
</script>

<template>
  <aside
    class="global-pet-host"
    :class="{ 'global-pet-host--minimized': minimized }"
    data-testid="global-pet-host"
    :data-pet-id="pet.petId"
    :data-pet-source="pet.source"
    aria-label="Streamora 全局宠物"
  >
    <div
      class="pet-bubble"
      role="status"
    >
      {{ minimized ? '我在这里' : `${pet.displayName}：一起看看今天的新视频吧！` }}
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
    <span
      v-if="activePetQuery.isError.value"
      class="pet-status"
    >静态伙伴模式</span>
  </aside>
</template>
