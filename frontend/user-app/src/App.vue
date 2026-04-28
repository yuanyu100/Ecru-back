<script setup>
import { computed, onBeforeUnmount, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import BottomNav from './components/BottomNav.vue';

const route = useRoute();
let themeMediaQuery = null;

const hiddenBottomNavRoutes = ['/login', '/register'];
const showBottomNav = computed(() => !hiddenBottomNavRoutes.includes(route.path));
const themeMigrationKey = 'ecru-theme-system-migrated';

const migrateLegacyTheme = () => {
  const hasMigrated = localStorage.getItem(themeMigrationKey) === 'true';
  const storedTheme = localStorage.getItem('ecru-theme');

  if (!hasMigrated && (!storedTheme || storedTheme === 'sand')) {
    localStorage.setItem('ecru-theme', 'system');
  }

  if (!hasMigrated) {
    localStorage.setItem(themeMigrationKey, 'true');
  }
};

const resolveTheme = (theme) => {
  if (theme === 'system') {
    return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'night' : 'sand';
  }
  return theme || 'sand';
};

const applyTheme = () => {
  const theme = localStorage.getItem('ecru-theme') || 'system';
  document.documentElement.dataset.theme = resolveTheme(theme);
};

const handleSystemThemeChange = () => {
  if ((localStorage.getItem('ecru-theme') || 'system') === 'system') {
    applyTheme();
  }
};

onMounted(() => {
  themeMediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
  migrateLegacyTheme();
  applyTheme();
  window.addEventListener('ecru-theme-change', applyTheme);
  themeMediaQuery.addEventListener('change', handleSystemThemeChange);
});

onBeforeUnmount(() => {
  window.removeEventListener('ecru-theme-change', applyTheme);
  themeMediaQuery?.removeEventListener('change', handleSystemThemeChange);
});
</script>

<template>
  <div class="app-container">
    <router-view v-slot="{ Component, route: currentRoute }">
      <KeepAlive>
        <component
          :is="Component"
          v-if="currentRoute.meta.keepAlive"
          :key="currentRoute.name || currentRoute.path"
        />
      </KeepAlive>
      <component
        :is="Component"
        v-if="!currentRoute.meta.keepAlive"
        :key="currentRoute.fullPath"
      />
    </router-view>
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
