<template>
  <section class="permission-denied">
    <strong>403</strong>
    <h1>当前角色无权访问</h1>
    <p>导航已经按权限收敛；直接访问未授权地址也会在路由层被拦截。</p>
    <RouterLink to="/">
      返回可访问的运营概览
    </RouterLink>
  </section>
</template>

<script setup lang="ts">
import { RouterLink } from 'vue-router'
</script>
