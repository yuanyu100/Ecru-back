<template>
  <div class="bottom-nav">
    <button
      v-for="item in navItems"
      :key="item.path"
      type="button"
      :class="['nav-item', isActive(item) ? 'active' : '']"
      @click="navigateTo(item.path)"
    >
      <span class="nav-icon">{{ item.icon }}</span>
      <span class="nav-text">{{ item.label }}</span>
    </button>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { authApi } from '../api/auth';

const router = useRouter();
const route = useRoute();

const navItems = computed(() => [
  { path: '/', label: '首页', icon: '首' },
  { path: '/chat', label: '对话', icon: '聊' },
  { path: '/wardrobe', label: '衣橱', icon: '柜' },
  { path: '/profile', label: '我的', icon: '我' }
]);

const navigateTo = (path) => {
  if ((path === '/profile' || path === '/chat' || path === '/wardrobe') && !authApi.isAuthenticated()) {
    router.push('/login');
    return;
  }

  router.push(path);
};

const isActive = (item) => {
  if (item.path === '/') {
    return route.path === '/';
  }

  if (item.path === '/chat') {
    return route.path === '/chat';
  }

  if (item.path === '/profile') {
    return (
      route.path.startsWith('/profile') ||
      ['/style-learning', '/saved-looks', '/materials', '/about'].includes(route.path)
    );
  }

  return route.path.startsWith(item.path);
};
</script>

<style scoped>
.bottom-nav {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: var(--bottom-nav-height);
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  background: color-mix(in srgb, var(--surface-strong) 92%, transparent);
  border-top: 1px solid var(--line-soft);
  box-shadow: 0 -8px 24px rgba(53, 41, 24, 0.06);
  backdrop-filter: blur(10px);
  z-index: 1000;
}

.nav-item {
  display: grid;
  place-items: center;
  gap: 2px;
  border: none;
  background: transparent;
  color: var(--text-faint);
  cursor: pointer;
}

.nav-item.active {
  color: var(--accent-strong);
  font-weight: 600;
}

.nav-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--accent-soft);
  font-size: 13px;
}

.nav-item.active .nav-icon {
  background: var(--accent-strong);
  color: var(--surface-strong);
}

.nav-text {
  font-size: 12px;
}

@media (min-width: 768px) {
  .bottom-nav {
    display: none;
  }
}
</style>
