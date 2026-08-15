<script setup lang="ts">
defineProps<{
  title: string
  description: string
}>()
</script>

<template>
  <section>
    <div class="page-title">
      <div>
        <span>OPERATIONS</span>
        <h1>{{ title }}</h1>
        <p>{{ description }}</p>
      </div>
    </div>
    <article class="panel module-placeholder">
      <strong>{{ title }}模块已预留</strong>
      <p>接口、权限、筛选、批量任务和审计能力将在对应里程碑按契约接入。</p>
    </article>
  </section>
</template>

