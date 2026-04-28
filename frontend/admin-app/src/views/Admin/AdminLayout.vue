<template>
  <div class="admin-layout">
    <aside class="admin-sidebar">
      <div class="sidebar-brand">
        <span class="sidebar-kicker">Ecru 后台</span>
        <strong>管理后台</strong>
        <p class="sidebar-copy">统一管理用户、衣橱资产、知识内容与 AI 运行状态。</p>
      </div>

      <nav class="sidebar-nav">
        <router-link
          v-for="item in navigationItems"
          :key="item.to"
          :to="item.to"
          class="nav-link"
        >
          <span class="nav-text">
            <span class="nav-title">{{ item.label }}</span>
            <span class="nav-subtitle">{{ item.description }}</span>
          </span>
          <span class="nav-index">{{ item.index }}</span>
        </router-link>
      </nav>

      <div class="sidebar-footer">
        <div class="identity-card">
          <span class="identity-overline">当前会话</span>
          <strong>{{ currentUser?.username || '当前用户' }}</strong>
          <span>{{ isAdmin ? '管理员账户' : '后台运营账户' }}</span>
        </div>
        <button class="ghost-button" type="button" @click="logout">退出登录</button>
      </div>
    </aside>

    <main class="admin-main">
      <header class="admin-header">
        <div>
          <p class="header-tag">Ecru 管理控制台</p>
          <h1>{{ pageTitle }}</h1>
          <p class="header-summary">{{ pageDescription }}</p>
        </div>

        <div class="header-meta">
          <span class="meta-pill brand-pill">{{ isAdmin ? '管理员模式' : '运营模式' }}</span>
          <span class="meta-pill">{{ currentDateLabel }}</span>
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
import { isAdminUser } from '../../utils/adminRole';

const route = useRoute();
const router = useRouter();
const currentUser = computed(() => authApi.getCurrentUser());
const isAdmin = computed(() => isAdminUser(currentUser.value));

const pageMetaMap = {
  dashboard: '查看核心指标、AI 运行状态与系统整体概览。',
  users: '维护用户状态，快速定位注册与角色信息。',
  clothing: '检查衣物台账、来源渠道与归属关系。',
  'style-images': '管理风格样例图片，保持推荐素材一致性。',
  knowledge: '维护面料、指南与洗护知识，支撑问答与检索。',
  'outfit-records': '查看 AI 搭配记录与用户反馈结果。',
  'api-monitor': '跟踪全用户调用量、成功率、延迟与错误分布。',
  'ai-conversations': '审查 AI 会话内容与使用轨迹。',
  'ai-prompts': '维护提示词配置，控制模型输出策略。'
};

const allNavigationItems = [
  { to: '/dashboard', label: '仪表盘', description: '系统总览与核心指标', index: '01', adminOnly: false },
  { to: '/users', label: '用户管理', description: '账号状态与检索', index: '02', adminOnly: true },
  { to: '/clothing', label: '衣橱管理', description: '衣物资产与归属', index: '03', adminOnly: false },
  { to: '/style-images', label: '风格图片', description: '风格样例素材库', index: '04', adminOnly: true },
  { to: '/knowledge', label: '知识库管理', description: '知识内容维护', index: '05', adminOnly: true },
  { to: '/outfit-records', label: '搭配记录', description: 'AI 推荐结果审查', index: '06', adminOnly: true },
  { to: '/api-monitor', label: 'AI 监控', description: '全用户调用与分类统计', index: '07', adminOnly: true },
  { to: '/ai-conversations', label: 'AI 会话', description: '对话内容追踪', index: '08', adminOnly: true },
  { to: '/ai-prompts', label: '提示词配置', description: '提示词与策略管理', index: '09', adminOnly: true }
];

const navigationItems = computed(() =>
  allNavigationItems.filter((item) => !item.adminOnly || isAdmin.value)
);

const pageTitle = computed(() => route.meta.title || '后台管理');
const pageDescription = computed(() => pageMetaMap[route.path.replace('/', '')] || '统一的后台管理工作台。');
const currentDateLabel = computed(() =>
  new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  }).format(new Date())
);

const logout = () => {
  authApi.logout();

  if (window.history.length > 1) {
    router.back();
    return;
  }

  router.replace('/login');
};
</script>
