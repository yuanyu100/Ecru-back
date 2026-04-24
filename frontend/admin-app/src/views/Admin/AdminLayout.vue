<template>
  <div class="admin-layout">
    <aside class="admin-sidebar">
      <div class="sidebar-brand">
        <p>Ecru</p>
        <strong>Admin</strong>
      </div>

      <nav class="sidebar-nav">
        <router-link to="/dashboard" class="nav-link">控制台</router-link>
        <router-link v-if="isAdmin" to="/users" class="nav-link">用户管理</router-link>
        <router-link to="/clothing" class="nav-link">衣物台账</router-link>
        <router-link v-if="isAdmin" to="/api-monitor" class="nav-link">AI 监控</router-link>
        <router-link v-if="isAdmin" to="/ai-conversations" class="nav-link">AI 会话管理</router-link>
        <router-link v-if="isAdmin" to="/knowledge" class="nav-link">知识库管理</router-link>
        <router-link v-if="isAdmin" to="/outfit-records" class="nav-link">穿搭记录管理</router-link>
      </nav>

      <div class="sidebar-footer">
        <div class="identity-card">
          <strong>{{ currentUser?.username || 'unknown' }}</strong>
          <span>{{ isAdmin ? '管理员' : '普通用户' }}</span>
        </div>
        <button class="ghost-button" type="button" @click="logout">退出登录</button>
      </div>
    </aside>

    <main class="admin-main">
      <header class="admin-header">
        <div>
          <p class="header-tag">后台工作区</p>
          <h1>{{ pageTitle }}</h1>
        </div>
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

const route = useRoute();
const router = useRouter();
const currentUser = computed(() => authApi.getCurrentUser());
const isAdmin = computed(() => currentUser.value?.role === 'ADMIN');
const pageTitle = computed(() => route.meta.title || '控制台');

const logout = () => {
  authApi.logout();
  router.push('/login');
};
</script>
