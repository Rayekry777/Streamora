<template>
  <section class="section-block page-hero">
    <span class="eyebrow">EXPLORE</span>
    <h1>发现感兴趣的内容</h1>
    <p>阶段 7 将在这里接入搜索、推荐与关注流；当前页面用于确认路由和用户端布局骨架。</p>
    <div
      class="chip-row"
      aria-label="内容分类"
    >
      <button
        v-for="label in ['推荐', '生活', '萌宠', '科技', '游戏', '知识']"
        :key="label"
        type="button"
      >
        {{ label }}
      </button>
    </div>
  </section>
</template>

