<template>
  <div class="bottom-nav">
    <button
      v-for="item in navItems"
      :key="item.path"
      type="button"
      :class="['nav-item', isActive(item) ? 'active' : '']"
      :aria-label="item.label"
      :title="item.label"
      @click="navigateTo(item.path)"
    >
      <span class="nav-icon-wrap">
        <svg v-if="item.id === 'home'" class="nav-icon" viewBox="0 0 24 24" fill="none" aria-hidden="true">
          <path
            d="M4.75 10.25L12 4.5l7.25 5.75v8a1.5 1.5 0 0 1-1.5 1.5h-3.5v-5h-4.5v5h-3.5a1.5 1.5 0 0 1-1.5-1.5z"
          />
        </svg>
        <svg v-else-if="item.id === 'chat'" class="nav-icon" viewBox="0 0 24 24" fill="none" aria-hidden="true">
          <path
            d="M6.75 7.75h10.5a2 2 0 0 1 2 2v5.5a2 2 0 0 1-2 2H11l-4.25 3v-3H6.75a2 2 0 0 1-2-2v-5.5a2 2 0 0 1 2-2z"
          />
        </svg>
        <svg v-else-if="item.id === 'wardrobe'" class="nav-icon" viewBox="0 0 24 24" fill="none" aria-hidden="true">
          <path
            d="M12 6.25a2.25 2.25 0 1 0-2.13-3h4.26A2.25 2.25 0 0 0 12 6.25zm0 0v1.35l6.25 4.58-1.22 1.57-1.78-1.3v7.3h-6.5v-7.3l-1.78 1.3-1.22-1.57L12 7.6"
          />
        </svg>
        <svg v-else class="nav-icon" viewBox="0 0 24 24" fill="none" aria-hidden="true">
          <path
            d="M12 12.25a3.25 3.25 0 1 0-3.25-3.25A3.25 3.25 0 0 0 12 12.25zm-5.75 6a5.75 5.75 0 0 1 11.5 0"
          />
        </svg>
        <span v-if="item.id === 'home'" class="home-breath-dot" aria-hidden="true"></span>
      </span>
    </button>
  </div>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router';
import { authApi } from '../api/auth';

const router = useRouter();
const route = useRoute();

const navItems = [
  { id: 'home', path: '/', label: '\u9996\u9875' },
  { id: 'chat', path: '/chat', label: '\u804a\u5929' },
  { id: 'wardrobe', path: '/wardrobe', label: '\u8863\u67dc' },
  { id: 'profile', path: '/profile', label: '\u6211\u7684' }
];

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
  box-shadow: 0 -10px 30px rgba(53, 41, 24, 0.05);
  backdrop-filter: blur(14px);
  z-index: 1000;
}

.nav-item {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  color: var(--text-faint);
  cursor: pointer;
  transition: color 180ms ease, transform 180ms ease;
}

.nav-item.active {
  color: var(--text-primary);
}

.nav-item::after {
  content: '';
  position: absolute;
  left: 50%;
  bottom: 8px;
  width: 16px;
  height: 2px;
  border-radius: 999px;
  background: color-mix(in srgb, var(--text-primary) 78%, transparent);
  transform: translateX(-50%) scaleX(0);
  opacity: 0;
  transition: transform 180ms ease, opacity 180ms ease;
}

.nav-item.active::after {
  transform: translateX(-50%) scaleX(1);
  opacity: 1;
}

.nav-icon-wrap {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
}

.nav-icon {
  width: 22px;
  height: 22px;
  stroke: currentColor;
  stroke-width: 1.8;
  stroke-linecap: round;
  stroke-linejoin: round;
  transition: transform 180ms ease;
}

.nav-item.active .nav-icon {
  transform: translateY(-1px);
}

.home-breath-dot {
  position: absolute;
  top: 1px;
  right: 0;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: color-mix(in srgb, var(--accent-strong) 64%, white 12%);
  box-shadow: 0 0 0 0 rgba(126, 92, 56, 0.18);
  animation: home-breathe 3.6s ease-in-out infinite;
}

.nav-item.active .home-breath-dot {
  background: var(--accent-strong);
  box-shadow: 0 0 0 0 rgba(126, 92, 56, 0.24);
}

@keyframes home-breathe {
  0%,
  100% {
    transform: scale(0.92);
    opacity: 0.55;
    box-shadow: 0 0 0 0 rgba(126, 92, 56, 0.06);
  }

  50% {
    transform: scale(1.14);
    opacity: 0.9;
    box-shadow: 0 0 0 4px rgba(126, 92, 56, 0.02);
  }
}

@media (min-width: 768px) {
  .bottom-nav {
    display: none;
  }
}
</style>
