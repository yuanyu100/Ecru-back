<template>
  <div class="detail-page">
    <header class="page-header">
      <button class="icon-back" type="button" aria-label="返回" @click="goBack">
        <span></span>
      </button>
      <div>
        <p class="eyebrow">我的</p>
        <h1>系统设置</h1>
      </div>
    </header>

    <section class="card">
      <div class="section-head">
        <p class="section-caption">界面</p>
        <h2>主题设置</h2>
      </div>

      <div class="theme-row">
        <button
          v-for="item in themes"
          :key="item.value"
          :class="['theme-chip', currentTheme === item.value ? 'active' : '']"
          type="button"
          @click="switchTheme(item.value)"
        >
          {{ item.label }}
        </button>
      </div>
    </section>

    <section class="card">
      <div class="section-head">
        <p class="section-caption">首页</p>
        <h2>展示与提示语</h2>
      </div>

      <button class="menu-item" type="button" @click="goTo('/profile/system/home')">
        <div>
          <strong>首页轮播文案</strong>
          <span>分开管理轮播文案、PDF 导入和展示节奏</span>
        </div>
        <i class="arrow"></i>
      </button>
    </section>

    <section class="card">
      <div class="section-head">
        <p class="section-caption">内容</p>
        <h2>更多入口</h2>
      </div>

      <button class="menu-item" type="button" @click="goTo('/saved-looks')">
        <div>
          <strong>收藏灵感</strong>
          <span>查看你保存过的推荐</span>
        </div>
        <i class="arrow"></i>
      </button>

      <button class="menu-item" type="button" @click="goTo('/materials')">
        <div>
          <strong>面料百科</strong>
          <span>阅读基础知识与检索文档</span>
        </div>
        <i class="arrow"></i>
      </button>

      <button class="menu-item" type="button" @click="goTo('/style-learning')">
        <div>
          <strong>风格灵感</strong>
          <span>继续训练你的穿搭偏好</span>
        </div>
        <i class="arrow"></i>
      </button>

      <button class="menu-item" type="button" @click="goTo('/about')">
        <div>
          <strong>关于我们</strong>
          <span>查看产品介绍</span>
        </div>
        <i class="arrow"></i>
      </button>
    </section>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';

const router = useRouter();
const currentTheme = ref(localStorage.getItem('ecru-theme') || 'system');

const themes = [
  { label: '跟随系统', value: 'system' },
  { label: '米黄', value: 'sand' },
  { label: '冷灰', value: 'cool' },
  { label: '暗夜', value: 'night' }
];

const switchTheme = (theme) => {
  currentTheme.value = theme;
  localStorage.setItem('ecru-theme', theme);
  window.dispatchEvent(new Event('ecru-theme-change'));
};

const goTo = (path) => {
  router.push(path);
};

const goBack = () => {
  router.push('/profile');
};
</script>

<style scoped>
.detail-page {
  min-height: 100vh;
  padding: 20px 16px 40px;
  background:
    radial-gradient(circle at top, rgba(255, 252, 246, 0.9), transparent 28%),
    linear-gradient(180deg, var(--bg-base) 0%, var(--bg-soft) 100%);
}

.page-header {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 18px;
}

.icon-back {
  display: inline-grid;
  place-items: center;
  width: 32px;
  height: 32px;
  border: 1px solid var(--line-soft);
  border-radius: 50%;
  background: color-mix(in srgb, var(--surface-strong) 90%, transparent);
  cursor: pointer;
}

.icon-back span {
  width: 10px;
  height: 10px;
  border-left: 1.5px solid var(--text-main);
  border-bottom: 1.5px solid var(--text-main);
  transform: rotate(45deg);
  margin-left: 4px;
}

.eyebrow,
.section-caption {
  color: var(--text-faint);
  font-size: 10px;
  letter-spacing: 0.12em;
}

.card {
  margin-top: 12px;
  padding: 16px;
  border-radius: 22px;
  border: 1px solid var(--line-soft);
  background: color-mix(in srgb, var(--surface-strong) 92%, transparent);
  box-shadow: var(--shadow-card);
}

.section-head h2 {
  margin-top: 4px;
  font-size: 15px;
}

.theme-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
}

.theme-chip {
  border: 1px solid var(--line-soft);
  border-radius: 999px;
  padding: 8px 14px;
  background: var(--surface-strong);
  color: var(--text-soft);
  font-size: 11px;
  cursor: pointer;
}

.theme-chip.active {
  border-color: var(--accent-strong);
  background: var(--accent-strong);
  color: var(--surface-strong);
}

.menu-item {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 0;
  border: none;
  border-bottom: 1px solid var(--line-soft);
  background: transparent;
  text-align: left;
  cursor: pointer;
}

.menu-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.menu-item:first-of-type {
  margin-top: 8px;
}

.menu-item strong {
  display: block;
  color: var(--text-main);
  font-size: 13px;
}

.menu-item span {
  display: block;
  margin-top: 5px;
  color: var(--text-soft);
  font-size: 10px;
  line-height: 1.55;
}

.arrow {
  flex: none;
  width: 9px;
  height: 9px;
  border-top: 1.5px solid var(--text-faint);
  border-right: 1.5px solid var(--text-faint);
  transform: rotate(45deg);
}

@media (min-width: 768px) {
  .detail-page {
    padding: 28px 24px 40px;
  }
}
</style>
