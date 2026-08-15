<script setup lang="ts">
import { AlertCircle, RotateCw } from 'lucide-vue-next'
import { nextTick, onBeforeUnmount, ref, watch } from 'vue'
import type { VideoPlayback } from '../../api/videoApi'

const props = defineProps<{
  playback: VideoPlayback
}>()

const emit = defineEmits<{
  playbackChange: [state: 'playing' | 'paused' | 'buffering']
}>()

const playerRoot = ref<HTMLElement>()
const unavailable = ref(false)
let player: { destroy: () => void; on: (event: string, callback: () => void) => void } | undefined

function dispose(): void {
  player?.destroy()
  player = undefined
}

async function mountPlayer(): Promise<void> {
  dispose()
  unavailable.value = false
  await nextTick()
  if (!playerRoot.value) return

  try {
    const [{ default: Player }, { default: HlsPlugin }] = await Promise.all([
      import('xgplayer'),
      import('xgplayer-hls'),
      import('xgplayer/dist/index.min.css'),
    ])
    const instance = new Player({
      el: playerRoot.value,
      url: props.playback.manifestUrl,
      poster: props.playback.posterUrl,
      autoplay: false,
      fluid: true,
      lang: 'zh-cn',
      plugins: [HlsPlugin],
      playbackRate: [0.75, 1, 1.25, 1.5, 2],
    }) as unknown as NonNullable<typeof player>
    player = instance
    instance.on('play', () => emit('playbackChange', 'playing'))
    instance.on('pause', () => emit('playbackChange', 'paused'))
    instance.on('waiting', () => emit('playbackChange', 'buffering'))
  } catch {
    unavailable.value = true
  }
}

watch(() => props.playback.videoId, () => {
  void mountPlayer()
}, { immediate: true })

onBeforeUnmount(dispose)
</script>

<template>
  <section class="video-player">
    <div
      v-show="!unavailable"
      ref="playerRoot"
      class="video-player__mount"
      aria-label="视频播放器"
    />
    <div
      v-if="unavailable"
      class="video-player__fallback"
      role="status"
    >
      <AlertCircle :size="28" />
      <strong>播放器暂时不可用</strong>
      <span>请检查网络后重试。</span>
      <button
        type="button"
        @click="mountPlayer"
      >
        <RotateCw :size="16" />
        重试
      </button>
    </div>
  </section>
</template>
