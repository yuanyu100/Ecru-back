<template>
  <div class="admin-layout">
    <aside class="admin-sidebar">
      <div class="sidebar-brand">
        <strong>管理后台</strong>
      </div>

      <nav class="sidebar-nav">
        <router-link
          v-for="item in navigationItems"
          :key="item.to"
          :to="item.to"
          class="nav-link"
        >
          <span class="nav-title">{{ item.label }}</span>
        </router-link>
      </nav>

      <div class="sidebar-footer">
        <strong class="sidebar-user">{{ currentUser?.username || '当前用户' }}</strong>
        <button class="ghost-button" type="button" @click="logout">退出登录</button>
      </div>
    </aside>

    <main class="admin-main">
      <header class="admin-header">
        <h1>{{ pageTitle }}</h1>
      </header>

      <section class="admin-body">
        <router-view />
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { authApi } from '../../api/auth';
import { isAdminUser } from '../../utils/adminRole';

const route = useRoute();
const router = useRouter();
const currentUser = computed(() => authApi.getCurrentUser());
const isAdmin = computed(() => isAdminUser(currentUser.value));

const allNavigationItems = [
  { to: '/dashboard', label: '仪表盘', adminOnly: false },
  { to: '/users', label: '用户', adminOnly: true },
  { to: '/clothing', label: '衣物', adminOnly: false },
  { to: '/style-images', label: '风格图', adminOnly: true },
  { to: '/knowledge', label: '知识库', adminOnly: true },
  { to: '/outfit-records', label: '搭配记录', adminOnly: true },
  { to: '/api-monitor', label: 'AI 监控', adminOnly: true },
  { to: '/ai-conversations', label: 'AI 会话', adminOnly: true },
  { to: '/ai-prompts', label: '提示词', adminOnly: true }
];

const navigationItems = computed(() =>
  allNavigationItems.filter((item) => !item.adminOnly || isAdmin.value)
);

const pageTitle = computed(() => route.meta.title || '后台管理');

const logout = () => {
  authApi.logout();

  if (window.history.length > 1) {
    router.back();
    return;
  }

  router.replace('/login');
};
</script>
