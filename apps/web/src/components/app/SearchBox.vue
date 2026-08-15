<script setup lang="ts">
import { Search } from 'lucide-vue-next'
import { ref, watch } from 'vue'

const props = withDefaults(defineProps<{
  modelValue?: string
  compact?: boolean
}>(), {
  modelValue: '',
  compact: false,
})

const emit = defineEmits<{
  submit: [keyword: string]
}>()

const keyword = ref(props.modelValue)

watch(() => props.modelValue, (value) => {
  keyword.value = value
})

function submit(): void {
  const normalized = keyword.value.trim()
  if (normalized) emit('submit', normalized)
}
</script>

<template>
  <form
    class="search-box"
    :class="{ 'search-box--compact': compact }"
    role="search"
    @submit.prevent="submit"
  >
    <input
      v-model="keyword"
      type="search"
      placeholder="搜索视频、创作者和分区"
      aria-label="搜索视频、创作者和分区"
    >
    <button
      type="submit"
      aria-label="提交搜索"
      title="搜索"
    >
      <Search :size="18" />
    </button>
  </form>
</template>
