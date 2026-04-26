<template>
  <div class="detail-page">
    <header class="page-header">
      <button class="icon-back" type="button" aria-label="返回" @click="goBack">
        <span></span>
      </button>
      <div>
        <p class="eyebrow">系统设置</p>
        <h1>首页轮播文案</h1>
      </div>
    </header>

    <div v-if="isLoading" class="state-card">正在读取首页配置...</div>

    <template v-else>
      <section class="card hero-card">
        <div class="hero-head">
          <div>
            <p class="section-caption">当前状态</p>
            <h2>{{ activeCount }} 句启用中</h2>
          </div>
          <span class="hero-badge">{{ totalCount }} 条文案</span>
        </div>
        <p class="hero-note">{{ activePreview || defaultPrompt }}</p>
        <div class="hero-metrics">
          <article class="metric-chip">
            <strong>{{ activeCount }}</strong>
            <span>参与轮播</span>
          </article>
          <article class="metric-chip">
            <strong>{{ homeFlowDefaultVisible ? '开' : '关' }}</strong>
            <span>推荐默认展开</span>
          </article>
          <article class="metric-chip">
            <strong>{{ (homePromptStayMs / 1000).toFixed(homePromptStayMs % 1000 ? 1 : 0) }}s</strong>
            <span>每句停留</span>
          </article>
        </div>
      </section>

      <section class="card">
        <div class="section-head">
          <p class="section-caption">文案</p>
          <h2>轮播内容</h2>
        </div>

        <button class="menu-item" type="button" @click="goTo('/profile/system/home/prompts')">
          <div>
            <strong>管理轮播文案</strong>
            <span>共 {{ totalCount }} 条，其中 {{ activeCount }} 条参与首页轮播</span>
          </div>
          <i class="arrow"></i>
        </button>

        <button class="menu-item" type="button" @click="goTo('/profile/system/home/import')">
          <div>
            <strong>从 PDF 导入</strong>
            <span>上传 PDF，解析成短句后再导入到轮播文案</span>
          </div>
          <i class="arrow"></i>
        </button>
      </section>

      <section class="card">
        <div class="section-head">
          <p class="section-caption">展示</p>
          <h2>节奏与默认状态</h2>
        </div>

        <button class="menu-item solo-item" type="button" @click="goTo('/profile/system/home/display')">
          <div>
            <strong>展示节奏</strong>
            <span>
              停留 {{ (homePromptStayMs / 1000).toFixed(homePromptStayMs % 1000 ? 1 : 0) }} 秒，
              淡入淡出 {{ (homePromptFadeMs / 1000).toFixed(homePromptFadeMs % 1000 ? 2 : 0) }} 秒
            </span>
          </div>
          <i class="arrow"></i>
        </button>

        <div class="status-row">
          <strong>今日推荐默认展开</strong>
          <span>{{ homeFlowDefaultVisible ? '开启' : '关闭' }}</span>
        </div>
      </section>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { authApi } from '../api/auth';
import { defaultPrompt, normalizeHomePromptSettings } from '../utils/homePromptSettings';

const router = useRouter();

const isLoading = ref(true);
const items = ref([]);
const homeFlowDefaultVisible = ref(true);
const homePromptStayMs = ref(2200);
const homePromptFadeMs = ref(820);

const totalCount = computed(() => items.value.length);
const activeItems = computed(() => items.value.filter((item) => item.enabled));
const activeCount = computed(() => activeItems.value.length);
const activePreview = computed(() => activeItems.value.map((item) => item.text).slice(0, 3).join(' / '));

const loadPage = async () => {
  isLoading.value = true;
  try {
    const response = await authApi.getHomePromptSettings();
    const settings = normalizeHomePromptSettings(response.data || {});
    items.value = settings.items;
    homeFlowDefaultVisible.value = settings.homeFlowDefaultVisible;
    homePromptStayMs.value = settings.homePromptStayMs;
    homePromptFadeMs.value = settings.homePromptFadeMs;
  } catch (error) {
    console.error('Load home settings overview failed:', error);
    alert(error.response?.data?.message || '读取首页配置失败');
  } finally {
    isLoading.value = false;
  }
};

const goTo = (path) => {
  router.push(path);
};

const goBack = () => {
  router.push('/profile/system');
};

onMounted(loadPage);
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

.card,
.state-card {
  margin-top: 12px;
  padding: 16px;
  border-radius: 22px;
  border: 1px solid var(--line-soft);
  background: color-mix(in srgb, var(--surface-strong) 92%, transparent);
  box-shadow: var(--shadow-card);
}

.state-card {
  text-align: center;
  color: var(--text-soft);
  font-size: 12px;
}

.hero-card {
  background:
    radial-gradient(circle at top left, rgba(214, 193, 164, 0.22), transparent 40%),
    color-mix(in srgb, var(--surface-strong) 94%, transparent);
}

.hero-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.section-head h2,
.hero-card h2 {
  margin-top: 4px;
  font-size: 15px;
}

.hero-badge {
  flex: none;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(194, 168, 130, 0.14);
  color: var(--accent-strong);
  font-size: 10px;
}

.hero-note {
  margin-top: 10px;
  color: var(--text-soft);
  font-size: 12px;
  line-height: 1.7;
}

.hero-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  margin-top: 14px;
}

.metric-chip {
  padding: 10px 12px;
  border-radius: 16px;
  background: rgba(255, 252, 246, 0.72);
}

.metric-chip strong {
  display: block;
  color: var(--text-main);
  font-size: 13px;
}

.metric-chip span {
  display: block;
  margin-top: 4px;
  color: var(--text-faint);
  font-size: 10px;
  line-height: 1.45;
}

.menu-item {
  width: 100%;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: start;
  gap: 12px;
  padding: 14px 0;
  border: none;
  border-bottom: 1px solid var(--line-soft);
  background: transparent;
  text-align: left;
  cursor: pointer;
}

.menu-item:last-of-type {
  border-bottom: none;
  padding-bottom: 0;
}

.menu-item:first-of-type {
  margin-top: 8px;
}

.solo-item {
  padding-bottom: 14px;
}

.menu-item strong,
.status-row strong {
  display: block;
  color: var(--text-main);
  font-size: 13px;
}

.menu-item span,
.status-row span {
  display: block;
  margin-top: 5px;
  color: var(--text-soft);
  font-size: 10px;
  line-height: 1.55;
}

.status-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid var(--line-soft);
}

.status-row span {
  margin-top: 0;
}

.arrow {
  flex: none;
  align-self: center;
  width: 9px;
  height: 9px;
  border-top: 1.5px solid var(--text-faint);
  border-right: 1.5px solid var(--text-faint);
  transform: rotate(45deg);
}

@media (max-width: 640px) {
  .hero-metrics {
    grid-template-columns: 1fr;
  }
}

@media (min-width: 768px) {
  .detail-page {
    padding: 28px 24px 40px;
  }
}
</style>
