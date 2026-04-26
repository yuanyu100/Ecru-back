<template>
  <div class="saved-page">
    <header class="saved-header">
      <button class="icon-button" type="button" @click="goHome" aria-label="返回">
        <span></span>
      </button>
      <div>
        <p class="header-caption">收藏灵感</p>
        <h1>留住想穿的样子</h1>
      </div>
    </header>

    <section class="saved-section">
      <div class="section-head">
        <div>
          <p class="section-caption">首页灵感</p>
          <h2>我点过喜欢的灵感</h2>
        </div>
        <button v-if="localLooks.length" class="text-button" type="button" @click="clearLocalLooks">清空</button>
      </div>

      <div v-if="localLooks.length" class="saved-grid">
        <article v-for="item in localLooks" :key="item.id" class="saved-card">
          <div class="polaroid-visual" :style="visualStyle(item.palette)">
            <span>{{ item.mood }}</span>
          </div>
          <div class="saved-copy">
            <strong>{{ item.title }}</strong>
            <p>{{ item.note }}</p>
          </div>
        </article>
      </div>
      <div v-else class="empty-shell">
        <p>还没有收藏首页灵感。</p>
      </div>
    </section>

    <section class="saved-section">
      <div class="section-head">
        <div>
          <p class="section-caption">穿搭方案</p>
          <h2>已收藏的历史方案</h2>
        </div>
        <button v-if="!isAuthenticated" class="text-button" type="button" @click="goLogin">登录后同步</button>
      </div>

      <div v-if="isAuthenticated && favoriteLooks.length" class="history-list">
        <article
          v-for="item in favoriteLooks"
          :key="item.id"
          class="history-item"
          @click="openOutfitDetail(item.id)"
        >
          <div>
            <strong>{{ item.outfitName }}</strong>
            <p>{{ item.outfitDescription || '暂无描述' }}</p>
          </div>
          <span>{{ formatDate(item.updatedAt || item.createdAt) }}</span>
        </article>
      </div>
      <div v-else class="empty-shell">
        <p>{{ isAuthenticated ? '当前还没有收藏的穿搭方案。' : '登录后这里会显示你收藏过的搭配方案。' }}</p>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { authApi } from '../api/auth';
import { outfitApi } from '../api/outfit';

const router = useRouter();
const historyItems = ref([]);
const localLooks = ref([]);

const isAuthenticated = computed(() => authApi.isAuthenticated());
const favoriteLooks = computed(() => historyItems.value.filter((item) => item.isFavorite));

const loadLocalLooks = () => {
  try {
    localLooks.value = JSON.parse(localStorage.getItem('savedLooks') || '[]');
  } catch {
    localLooks.value = [];
  }
};

const loadFavoriteLooks = async () => {
  if (!isAuthenticated.value) {
    historyItems.value = [];
    return;
  }

  try {
    const response = await outfitApi.getHistory(1, 30);
    historyItems.value = response.data || [];
  } catch (error) {
    console.error('Load favorite looks failed:', error);
  }
};

const clearLocalLooks = () => {
  localStorage.removeItem('savedLooks');
  loadLocalLooks();
};

const goHome = () => router.push('/');
const goLogin = () => router.push('/login');
const openOutfitDetail = (id) => router.push(`/outfit/history/${id}`);

const formatDate = (value) => {
  if (!value) {
    return '刚刚';
  }

  return new Date(value).toLocaleDateString('zh-CN', {
    month: '2-digit',
    day: '2-digit'
  });
};

const visualStyle = (palette = []) => {
  const colors = Array.isArray(palette) && palette.length ? palette : ['#ded2bd', '#f4eee3'];
  return {
    background: `linear-gradient(135deg, ${colors[0]}, ${colors[1] || colors[0]})`
  };
};

onMounted(async () => {
  loadLocalLooks();
  await loadFavoriteLooks();
});
</script>

<style scoped>
.saved-page {
  min-height: 100vh;
  padding: 22px 18px 96px;
  background:
    radial-gradient(circle at top, rgba(255, 248, 238, 0.92), transparent 30%),
    linear-gradient(180deg, var(--bg-base) 0%, var(--bg-soft) 100%);
}

.saved-header,
.section-head,
.history-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.saved-header {
  gap: 14px;
  align-items: flex-start;
}

.header-caption,
.section-caption {
  color: var(--text-faint);
  font-size: 12px;
  letter-spacing: 0.12em;
}

.saved-header h1,
.section-head h2 {
  margin-top: 6px;
  color: var(--text-main);
}

.saved-section {
  margin-top: 28px;
}

.saved-grid,
.history-list {
  display: grid;
  gap: 14px;
  margin-top: 16px;
}

.saved-card,
.history-item,
.empty-shell {
  border-radius: 24px;
  border: 1px solid var(--line-soft);
  background: var(--surface);
  box-shadow: var(--shadow-card);
}

.saved-card {
  padding: 14px;
}

.polaroid-visual {
  position: relative;
  aspect-ratio: 4 / 5;
  border-radius: 18px;
  overflow: hidden;
}

.polaroid-visual span {
  position: absolute;
  left: 16px;
  bottom: 16px;
  color: rgba(62, 52, 38, 0.82);
  font-family: 'Iowan Old Style', 'Noto Serif SC', 'Songti SC', serif;
  font-size: 18px;
}

.saved-copy {
  margin-top: 14px;
}

.saved-copy strong,
.history-item strong {
  color: var(--text-main);
}

.saved-copy p,
.history-item p,
.history-item span,
.empty-shell p {
  margin-top: 8px;
  color: var(--text-soft);
  line-height: 1.7;
}

.history-item {
  gap: 12px;
  padding: 16px 18px;
  cursor: pointer;
}

.history-item span {
  margin-top: 0;
  font-size: 12px;
  white-space: nowrap;
}

.empty-shell {
  margin-top: 16px;
  padding: 28px 20px;
  text-align: center;
}

.icon-button,
.text-button {
  border: none;
  background: transparent;
  cursor: pointer;
}

.icon-button {
  display: inline-grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  border: 1px solid var(--line-soft);
  background: color-mix(in srgb, var(--surface-strong) 90%, transparent);
}

.icon-button span {
  width: 10px;
  height: 10px;
  border-left: 1.5px solid var(--text-main);
  border-bottom: 1.5px solid var(--text-main);
  transform: rotate(45deg);
  margin-left: 4px;
}

.text-button {
  color: var(--accent);
}

@media (min-width: 768px) {
  .saved-page {
    padding: 30px 28px 42px;
  }

  .saved-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
