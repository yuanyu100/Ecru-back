<template>
  <div class="admin-layout">
    <aside class="admin-sidebar">
      <div class="sidebar-brand">
        <p>Ecru</p>
        <strong>管理后台</strong>
      </div>

      <nav class="sidebar-nav">
        <router-link to="/dashboard" class="nav-link">仪表盘</router-link>
        <router-link v-if="isAdmin" to="/users" class="nav-link">用户管理</router-link>
        <router-link to="/clothing" class="nav-link">衣橱管理</router-link>
        <router-link v-if="isAdmin" to="/style-images" class="nav-link">风格图片</router-link>
        <router-link v-if="isAdmin" to="/knowledge" class="nav-link">知识库管理</router-link>
        <router-link v-if="isAdmin" to="/outfit-records" class="nav-link">搭配记录</router-link>
        <router-link v-if="isAdmin" to="/api-monitor" class="nav-link">AI 监控</router-link>
        <router-link v-if="isAdmin" to="/ai-conversations" class="nav-link">AI 会话</router-link>
        <router-link v-if="isAdmin" to="/ai-prompts" class="nav-link">Prompt 配置</router-link>
      </nav>

      <div class="sidebar-footer">
        <div class="identity-card">
          <strong>{{ currentUser?.username || 'unknown' }}</strong>
          <span>{{ isAdmin ? '管理员账号' : '普通后台账号' }}</span>
        </div>
        <button class="ghost-button" type="button" @click="logout">退出登录</button>
      </div>
    </aside>

    <main class="admin-main">
      <header class="admin-header">
        <div>
          <p class="header-tag">Ecru Admin Console</p>
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
