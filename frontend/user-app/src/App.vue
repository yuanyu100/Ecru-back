<script setup>
import { computed, onBeforeUnmount, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import BottomNav from './components/BottomNav.vue';

const route = useRoute();

const hiddenBottomNavRoutes = ['/login', '/register'];
const showBottomNav = computed(() => !hiddenBottomNavRoutes.includes(route.path));

const applyTheme = () => {
  const theme = localStorage.getItem('ecru-theme') || 'sand';
  document.documentElement.dataset.theme = theme;
};

onMounted(() => {
  applyTheme();
  window.addEventListener('ecru-theme-change', applyTheme);
});

onBeforeUnmount(() => {
  window.removeEventListener('ecru-theme-change', applyTheme);
});
</script>

<template>
  <div class="app-container">
    <router-view />
    <BottomNav v-if="showBottomNav" />
  </div>
</template>

<style scoped>
.app-container {
  min-height: 100vh;
  padding-bottom: var(--app-bottom-offset);
  background: var(--bg-base);
}

@media (min-width: 768px) {
  .app-container {
    padding-bottom: 0;
  }
}
</style>
