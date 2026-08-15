<script setup lang="ts">
const metrics = [
  { label: '待审核内容', value: '—', hint: '阶段 4 接入', tone: 'violet' },
  { label: '转码任务', value: '—', hint: '阶段 3 接入', tone: 'blue' },
  { label: 'AI 调用成本', value: '—', hint: '阶段 6 接入', tone: 'coral' },
  { label: '在线服务', value: '17', hint: '阶段 1 目标', tone: 'green' },
]
</script>

<template>
  <section>
    <div class="page-title">
      <div>
        <span>OVERVIEW</span>
        <h1>运营概览</h1>
        <p>当前为管理端基础骨架，真实读模型在对应领域阶段接入。</p>
      </div>
      <button
        class="admin-primary"
        type="button"
        disabled
      >
        导出日报
      </button>
    </div>

    <div class="metric-grid">
      <article
        v-for="metric in metrics"
        :key="metric.label"
        class="metric-card"
        :data-tone="metric.tone"
      >
        <span>{{ metric.label }}</span>
        <strong>{{ metric.value }}</strong>
        <small>{{ metric.hint }}</small>
      </article>
    </div>

    <div class="dashboard-grid">
      <article class="panel">
        <div class="panel-heading">
          <strong>系统动态</strong>
          <span>最近 24 小时</span>
        </div>
        <div class="empty-panel">
          接入领域事件后显示审核、发布和模型路由动态。
        </div>
      </article>
      <article class="panel">
        <div class="panel-heading">
          <strong>高风险操作</strong>
          <span>需要复验</span>
        </div>
        <div class="empty-panel">
          阶段 2 接入密码复验和一次性动作令牌。
        </div>
      </article>
    </div>
  </section>
</template>

