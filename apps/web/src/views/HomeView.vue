<script setup lang="ts">
import { RouterLink } from 'vue-router'

const videos = [
  { id: 'demo-1', title: '和宠物一起探索城市角落', creator: 'Streamora Studio', tone: 'violet' },
  { id: 'demo-2', title: '三分钟做一份治愈早餐', creator: '清晨厨房', tone: 'coral' },
  { id: 'demo-3', title: '猫咪第一次看海是什么反应', creator: '海边日记', tone: 'blue' },
]
</script>

<template>
  <section class="hero-section">
    <div>
      <span class="eyebrow">VIDEO × AI COMPANION</span>
      <h1>不只是观看，<br>让宠物陪你一起感受。</h1>
      <p>Streamora 把视频社区与可成长的 AI 宠物连接在一起。它会理解播放情境，也会记住你允许它记住的事。</p>
      <div class="hero-actions">
        <RouterLink
          class="primary-button"
          to="/explore"
        >
          开始探索
        </RouterLink>
        <RouterLink
          class="ghost-button"
          to="/upload"
        >
          成为创作者
        </RouterLink>
      </div>
    </div>
    <div
      class="hero-card"
      aria-label="产品能力预览"
    >
      <div class="hero-card__screen">
        <span>AI PET REACTION</span>
        <strong>“这个转场好漂亮！”</strong>
      </div>
      <div class="hero-card__meta">
        <span>规则即时反应</span>
        <span>视频语义提示</span>
        <span>用户可控记忆</span>
      </div>
    </div>
  </section>

  <section class="section-block">
    <div class="section-heading">
      <div>
        <span class="eyebrow">TRENDING NOW</span>
        <h2>今天值得一看</h2>
      </div>
      <RouterLink to="/explore">
        查看全部 →
      </RouterLink>
    </div>

    <div class="video-grid">
      <RouterLink
        v-for="video in videos"
        :key="video.id"
        class="video-card"
        :to="`/watch/${video.id}`"
      >
        <div
          class="video-card__cover"
          :data-tone="video.tone"
        >
          <span class="video-card__duration">03:24</span>
        </div>
        <h3>{{ video.title }}</h3>
        <p>{{ video.creator }} · 1.2 万次观看</p>
      </RouterLink>
    </div>
  </section>
</template>

