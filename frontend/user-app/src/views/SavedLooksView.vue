<template>
  <div class="saved-page">
    <header class="saved-header">
      <button class="icon-button" type="button" aria-label="返回" @click="goBack">
        <span></span>
      </button>
      <div>
        <p class="header-caption">收藏灵感</p>
        <h1>把你想留下的搭配灵感都放在这里</h1>
      </div>
    </header>

    <section class="saved-section">
      <div class="section-head">
        <div>
          <p class="section-caption">本地收藏</p>
          <h2>推荐流里手动收藏的灵感卡片</h2>
        </div>
        <button v-if="localLooks.length" class="text-button" type="button" @click="clearLocalLooks">清空</button>
      </div>

      <div v-if="localLooks.length" class="saved-grid">
        <article
          v-for="item in localLooks"
          :key="`local-${item.id}`"
          :class="['look-card', 'interactive-card', canOpenSavedLook(item) ? '' : 'disabled']"
          :role="canOpenSavedLook(item) ? 'button' : undefined"
          :tabindex="canOpenSavedLook(item) ? 0 : -1"
          @click="openSavedLook(item)"
          @keydown.enter.prevent="openSavedLook(item)"
          @keydown.space.prevent="openSavedLook(item)"
        >
          <div class="look-board">
            <div
              v-for="(boardItem, index) in resolveBoardItems(item)"
              :key="`${item.id}-${boardItem.clothingId || boardItem.name || boardItem.category || index}`"
              :class="['board-piece', `piece-${index + 1}`]"
            >
              <img
                v-if="boardItem.imageUrl"
                :src="boardItem.imageUrl"
                :alt="boardItem.name || '搭配单品'"
                loading="lazy"
              />
              <div v-else class="piece-placeholder">
                <span>{{ shortCategory(boardItem.category || boardItem.name) }}</span>
              </div>
            </div>

            <div v-if="item.mood" class="board-stamp">
              <span>{{ item.mood }}</span>
            </div>
          </div>

          <div class="look-copy">
            <strong>{{ item.title || '收藏灵感' }}</strong>
            <p>{{ item.note || '从推荐流里留下的一张灵感卡片。' }}</p>
          </div>
        </article>
      </div>
      <div v-else class="empty-shell">
        <p>你还没有收藏灵感卡片，先去推荐流里挑几套留下来。</p>
      </div>
    </section>

    <section class="saved-section">
      <div class="section-head">
        <div>
          <p class="section-caption">方案收藏</p>
          <h2>你标记过收藏的搭配记录</h2>
        </div>
        <button v-if="!isAuthenticated" class="text-button" type="button" @click="goLogin">去登录</button>
      </div>

      <div v-if="isAuthenticated && favoriteLookCards.length" class="saved-grid">
        <article
          v-for="item in favoriteLookCards"
          :key="item.id"
          class="look-card interactive-card"
          role="button"
          tabindex="0"
          @click="openOutfitDetail(item.id)"
          @keydown.enter.prevent="openOutfitDetail(item.id)"
          @keydown.space.prevent="openOutfitDetail(item.id)"
        >
          <div class="look-board">
            <div
              v-for="(boardItem, index) in resolveBoardItems(item)"
              :key="`${item.id}-${boardItem.clothingId || boardItem.name || boardItem.category || index}`"
              :class="['board-piece', `piece-${index + 1}`]"
            >
              <img
                v-if="boardItem.imageUrl"
                :src="boardItem.imageUrl"
                :alt="boardItem.name || '搭配单品'"
                loading="lazy"
              />
              <div v-else class="piece-placeholder">
                <span>{{ shortCategory(boardItem.category || boardItem.name) }}</span>
              </div>
            </div>

            <div class="board-stamp board-stamp-muted">
              <span>{{ formatDate(item.updatedAt || item.createdAt) }}</span>
            </div>
          </div>

          <div class="look-copy">
            <strong>{{ item.outfitName || '收藏方案' }}</strong>
            <p>{{ item.outfitDescription || '点击查看完整搭配记录。' }}</p>
          </div>
        </article>
      </div>
      <div v-else class="empty-shell">
        <p>{{ isAuthenticated ? '你还没有收藏任何搭配记录。' : '登录后可以查看你收藏过的搭配记录。' }}</p>
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
const favoriteLookBoards = ref({});

const fallbackBoardItems = [
  { category: '上装', imageUrl: '' },
  { category: '下装', imageUrl: '' }
];

const isAuthenticated = computed(() => authApi.isAuthenticated());
const favoriteLooks = computed(() => historyItems.value.filter((item) => item.isFavorite));
const favoriteLookCards = computed(() =>
  favoriteLooks.value.map((item) => ({
    ...item,
    items: favoriteLookBoards.value[item.id] || []
  }))
);

const loadLocalLooks = () => {
  try {
    localLooks.value = JSON.parse(localStorage.getItem('savedLooks') || '[]');
  } catch {
    localLooks.value = [];
  }
};

const mapAdviceItemsToBoardItems = (items = []) =>
  items.slice(0, 4).map((item) => ({
    clothingId: item.id,
    name: item.itemName || '',
    category: item.itemCategory || '',
    imageUrl: item.itemImageUrl || ''
  }));

const loadFavoriteLookBoards = async (records = []) => {
  if (!records.length) {
    favoriteLookBoards.value = {};
    return;
  }

  const results = await Promise.allSettled(records.map((item) => outfitApi.getAdviceDetail(item.id)));
  const nextBoards = {};

  results.forEach((result, index) => {
    if (result.status !== 'fulfilled') {
      return;
    }

    const record = records[index];
    nextBoards[record.id] = Array.isArray(result.value.data?.items)
      ? mapAdviceItemsToBoardItems(result.value.data.items)
      : [];
  });

  favoriteLookBoards.value = nextBoards;
};

const loadFavoriteLooks = async () => {
  if (!isAuthenticated.value) {
    historyItems.value = [];
    favoriteLookBoards.value = {};
    return;
  }

  try {
    const response = await outfitApi.getHistory(1, 30);
    historyItems.value = response.data || [];
    await loadFavoriteLookBoards(favoriteLooks.value);
  } catch (error) {
    console.error('Load favorite looks failed:', error);
    historyItems.value = [];
    favoriteLookBoards.value = {};
  }
};

const clearLocalLooks = () => {
  localStorage.removeItem('savedLooks');
  loadLocalLooks();
};

const goBack = () => {
  if (window.history.length > 1) {
    router.back();
    return;
  }
  router.push('/profile/system');
};

const goLogin = () => router.push('/login');
const openOutfitDetail = (id) => router.push(`/outfit/history/${id}`);

const canOpenSavedLook = (item) => isAuthenticated.value && item?.id && !String(item.id).startsWith('guest-look-');

const openSavedLook = (item) => {
  if (!item?.id) {
    return;
  }

  if (!isAuthenticated.value) {
    router.push('/login');
    return;
  }

  if (String(item.id).startsWith('guest-look-')) {
    return;
  }

  router.push(`/home/recommendations/${item.id}`);
};

const formatDate = (value) => {
  if (!value) {
    return '最近收藏';
  }

  return new Date(value).toLocaleDateString('zh-CN', {
    month: '2-digit',
    day: '2-digit'
  });
};

const shortCategory = (category) => {
  const value = String(category || '').trim();
  return value || '单品';
};

const resolveBoardItems = (item) => {
  const sourceItems = Array.isArray(item?.items) ? item.items.filter(Boolean).slice(0, 4) : [];
  if (sourceItems.length >= 2) {
    return sourceItems;
  }

  return [...sourceItems, ...fallbackBoardItems].slice(0, Math.max(2, sourceItems.length));
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
.section-head {
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

.saved-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin-top: 16px;
}

.look-card,
.empty-shell {
  border-radius: 24px;
  border: 1px solid var(--line-soft);
  background: var(--surface-strong);
  box-shadow: var(--shadow-card);
}

.look-card {
  overflow: hidden;
  padding: 0;
}

.interactive-card {
  cursor: pointer;
}

.interactive-card:focus-visible {
  outline: 2px solid var(--accent-strong);
  outline-offset: 3px;
}

.interactive-card.disabled {
  cursor: default;
}

.look-board {
  position: relative;
  aspect-ratio: 1 / 1.02;
  margin: 16px;
  border-radius: 22px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(250, 247, 241, 0.98)),
    #fff;
  box-shadow:
    inset 0 0 0 1px rgba(170, 153, 130, 0.08),
    0 14px 32px rgba(122, 104, 82, 0.08);
}

.board-piece {
  position: absolute;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border-radius: 18px;
  background: #f5f1ea;
}

.board-piece img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.piece-1 {
  top: 14px;
  left: 14px;
  width: calc(50% - 20px);
  height: 44%;
}

.piece-2 {
  top: 14px;
  right: 14px;
  width: calc(50% - 20px);
  height: 52%;
}

.piece-3 {
  bottom: 14px;
  left: 14px;
  width: calc(44% - 12px);
  height: 30%;
}

.piece-4 {
  bottom: 14px;
  right: 14px;
  width: calc(56% - 16px);
  height: 22%;
}

.piece-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  color: var(--text-soft);
  font-size: 12px;
  letter-spacing: 0.08em;
}

.board-stamp {
  position: absolute;
  left: 18px;
  bottom: 18px;
  display: inline-flex;
  align-items: center;
  padding: 5px 10px;
  border-radius: 999px;
  background: rgba(255, 251, 245, 0.94);
  color: rgba(70, 58, 42, 0.88);
  font-family: 'Iowan Old Style', 'Noto Serif SC', 'Songti SC', serif;
  font-size: 13px;
}

.board-stamp-muted {
  background: rgba(255, 251, 245, 0.98);
  color: var(--text-soft);
  font-family: inherit;
  font-size: 12px;
}

.look-copy {
  padding: 0 18px 18px;
}

.look-copy strong {
  display: block;
  color: var(--text-main);
  font-size: 15px;
}

.look-copy p,
.empty-shell p {
  margin-top: 10px;
  color: var(--text-soft);
  line-height: 1.7;
}

.look-copy p {
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
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
}

@media (max-width: 560px) {
  .saved-page {
    padding: 20px 16px 96px;
  }

  .saved-grid {
    gap: 12px;
  }

  .look-copy {
    padding: 0 14px 16px;
  }

  .look-copy strong {
    font-size: 14px;
  }

  .look-copy p,
  .board-stamp,
  .board-stamp-muted {
    font-size: 12px;
  }
}
</style>
