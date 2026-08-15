<script setup lang="ts">
import { RouterView } from 'vue-router'
import AppHeader from './components/app/AppHeader.vue'
import GlobalPetHost from './pet/GlobalPetHost.vue'
</script>

<template>
  <div class="user-shell">
    <AppHeader />

    <main class="page-content">
      <RouterView />
    </main>

    <GlobalPetHost />
  </div>
</template>
