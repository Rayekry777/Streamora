<template>
  <div
    class="video-grid"
    aria-label="内容加载中"
    aria-busy="true"
  >
    <div
      v-for="index in 8"
      :key="index"
      class="content-skeleton"
    >
      <span class="content-skeleton__cover" />
      <span class="content-skeleton__line" />
      <span class="content-skeleton__line content-skeleton__line--short" />
    </div>
  </div>
</template>
