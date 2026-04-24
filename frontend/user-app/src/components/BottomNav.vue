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
  { path: '/materials', label: '材质', icon: '料' },
  { path: '/wardrobe', label: '衣橱', icon: '柜' },
  { path: '/profile', label: '我的', icon: '我' }
]);

const navigateTo = (path) => {
  if ((path === '/profile' || path === '/materials' || path === '/wardrobe') && !authApi.isAuthenticated()) {
    router.push('/login');
    return;
  }

  router.push(path);
};

const isActive = (item) => {
  if (item.path === '/') {
    return route.path === '/' || route.path === '/chat';
  }

  if (item.path === '/profile') {
    return ['/profile', '/style-learning', '/login', '/register'].includes(route.path);
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
  height: 64px;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  background: rgba(247, 236, 213, 0.96);
  border-top: 1px solid #e8d5a2;
  box-shadow: 0 -2px 16px rgba(87, 62, 25, 0.08);
  backdrop-filter: blur(10px);
  z-index: 1000;
}

.nav-item {
  display: grid;
  place-items: center;
  gap: 2px;
  border: none;
  background: transparent;
  color: #8b7355;
  cursor: pointer;
}

.nav-item.active {
  color: #6b4b1f;
  font-weight: 600;
}

.nav-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: rgba(107, 75, 31, 0.08);
  font-size: 13px;
}

.nav-item.active .nav-icon {
  background: #6b4b1f;
  color: #fff8ef;
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
