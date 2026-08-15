<script setup lang="ts">
import { useRoute } from 'vue-router'

const route = useRoute()
</script>

<template>
  <section class="watch-layout">
    <div>
      <div class="player-placeholder">
        <span>播放器骨架 · {{ route.params.videoId }}</span>
      </div>
      <h1>示例视频标题</h1>
      <p class="muted">
        阶段 3 接入 xgplayer、HLS 和弹幕；阶段 5 将宠物 Teleport 到全屏容器。
      </p>
    </div>
    <aside class="watch-panel">
      <strong>宠物反应时间线</strong>
      <p>字幕、关键帧与语义提示将在后续阶段显示在这里。</p>
    </aside>
  </section>
</template>

