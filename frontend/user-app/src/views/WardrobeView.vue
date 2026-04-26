<template>
  <div ref="wardrobePageRef" class="wardrobe-page">
    <section ref="filterStripRef" class="filter-strip">
      <div class="filter-copy">
        <p>先把常穿的收进来，再慢慢补齐整个衣橱。</p>
        <span class="count-copy">{{ totalItems }} 件</span>
      </div>

      <input
        v-model.trim="filters.keyword"
        type="text"
        placeholder="搜索衣物、颜色或材质"
        @keyup.enter="reloadList"
      />

      <div class="action-strip">
        <button class="line-button import-button" type="button" @click="navigateToImport">订单导入</button>
      </div>

      <div class="chip-row">
        <button
          v-for="item in categoryOptions"
          :key="item.value"
          :class="['filter-chip', filters.category === item.value ? 'active' : '']"
          type="button"
          @click="selectCategory(item.value)"
        >
          {{ item.label }}
        </button>
      </div>
    </section>

    <div class="content-area">
      <div v-if="isLoading" class="state-shell">正在读取衣橱...</div>
      <div v-else-if="clothingList.length === 0" class="state-shell">
        <p>衣橱里还没有内容。</p>
        <div class="empty-actions">
          <button class="line-button" type="button" @click="navigateToAdd">去添加第一件</button>
          <button class="line-button import-button" type="button" @click="navigateToImport">从订单导入</button>
        </div>
      </div>

      <section v-else class="wardrobe-grid">
        <article v-for="item in clothingList" :key="item.itemId" class="wardrobe-card" @click="navigateToDetail(item.itemId)">
          <div class="hanger-shell">
            <div class="hanger-mark"></div>
            <img :src="item.imageUrl || fallbackImage" :alt="item.name || '衣物图片'" />
          </div>
          <div class="card-copy">
            <strong>{{ item.name || '未命名衣物' }}</strong>
            <p>{{ item.category || '未分类' }}</p>
            <div class="tag-row">
              <span>#{{ item.material || '材质待补充' }}</span>
              <span>#{{ item.color?.primary || '颜色待补充' }}</span>
              <span>#频率{{ item.frequency || 1 }}</span>
            </div>
          </div>
        </article>
      </section>

      <footer v-if="totalPages > 1" class="pagination">
        <button type="button" class="line-button" :disabled="currentPage === 1" @click="changePage(currentPage - 1)">上一页</button>
        <span>{{ currentPage }} / {{ totalPages }}</span>
        <button type="button" class="line-button" :disabled="currentPage === totalPages" @click="changePage(currentPage + 1)">
          下一页
        </button>
      </footer>
    </div>

    <button class="fab-button" type="button" aria-label="添加衣物" @click="navigateToAdd">
      <span></span>
      <span></span>
    </button>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { wardrobeApi } from '../api/wardrobe';

const router = useRouter();
const wardrobePageRef = ref(null);
const filterStripRef = ref(null);
const clothingList = ref([]);
const isLoading = ref(false);
const currentPage = ref(1);
const pageSize = ref(12);
const totalItems = ref(0);
let layoutResizeObserver = null;

const fallbackImage =
  'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="320" height="420"><rect width="100%" height="100%" fill="%23f0e7d8"/><text x="50%" y="50%" text-anchor="middle" fill="%23847a68" font-size="24">LOOK</text></svg>';

const categoryOptions = [
  { label: '全部', value: '' },
  { label: '上装', value: '上装' },
  { label: '下装', value: '下装' },
  { label: '裙装', value: '裙装' },
  { label: '外套', value: '外套' }
];

const filters = reactive({
  category: '',
  keyword: ''
});

const totalPages = computed(() => Math.max(1, Math.ceil(totalItems.value / pageSize.value)));

const syncLayoutMetrics = async () => {
  await nextTick();
  const page = wardrobePageRef.value;
  if (!page) {
    return;
  }

  const filterHeight = Math.ceil(filterStripRef.value?.offsetHeight || 0);
  page.style.setProperty('--wardrobe-filter-height', `${filterHeight}px`);
};

const fetchClothingList = async () => {
  isLoading.value = true;
  try {
    const result = await wardrobeApi.getClothingList({
      page: currentPage.value,
      size: pageSize.value,
      category: filters.category || undefined,
      keyword: filters.keyword || undefined
    });

    clothingList.value = result.data?.items || [];
    totalItems.value = result.data?.total || 0;
  } catch (error) {
    console.error('Load wardrobe failed:', error);
    alert(error.response?.data?.message || '读取衣橱失败');
  } finally {
    isLoading.value = false;
  }
};

const reloadList = () => {
  currentPage.value = 1;
  fetchClothingList();
};

const selectCategory = (value) => {
  filters.category = value;
  reloadList();
};

const changePage = (page) => {
  currentPage.value = page;
  fetchClothingList();
};

const navigateToAdd = () => {
  router.push('/wardrobe/add');
};

const navigateToImport = () => {
  router.push('/wardrobe/import');
};

const navigateToDetail = (itemId) => {
  router.push(`/wardrobe/detail/${itemId}`);
};

onMounted(fetchClothingList);

onMounted(async () => {
  await syncLayoutMetrics();

  if (typeof ResizeObserver !== 'undefined') {
    layoutResizeObserver = new ResizeObserver(() => {
      syncLayoutMetrics();
    });

    if (filterStripRef.value) {
      layoutResizeObserver.observe(filterStripRef.value);
    }
  }
});

onBeforeUnmount(() => {
  layoutResizeObserver?.disconnect();
});
</script>

<style scoped>
.wardrobe-page {
  position: fixed;
  inset: 0 0 var(--app-bottom-offset) 0;
  --wardrobe-top-gap: 0px;
  --wardrobe-side-gap: 16px;
  --wardrobe-bottom-gap: 18px;
  --wardrobe-filter-height: 170px;
  padding: 0 16px 108px;
  overflow: hidden;
  background:
    radial-gradient(circle at top, rgba(255, 251, 245, 0.92), transparent 26%),
    linear-gradient(180deg, var(--bg-base) 0%, var(--bg-soft) 100%);
}

.filter-strip {
  position: absolute;
  top: var(--wardrobe-top-gap);
  left: var(--wardrobe-side-gap);
  right: var(--wardrobe-side-gap);
  z-index: 14;
  padding: 14px 0 14px;
  background:
    linear-gradient(180deg, color-mix(in srgb, var(--bg-base) 98%, transparent) 0%, color-mix(in srgb, var(--bg-base) 92%, transparent) 100%);
  backdrop-filter: blur(8px);
}

.filter-copy p {
  color: var(--text-faint);
  font-size: 11px;
}

.count-copy,
.card-copy p,
.state-shell p,
.pagination span {
  color: var(--text-soft);
}

.count-copy {
  display: inline-block;
  margin-top: 4px;
  font-size: 11px;
}

.filter-strip input {
  width: 100%;
  margin-top: 12px;
  border: 1px solid var(--line-soft);
  border-radius: 18px;
  padding: 11px 14px;
  background: color-mix(in srgb, var(--surface-strong) 92%, transparent);
  color: var(--text-main);
  font-size: 12px;
  outline: none;
}

.action-strip {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

.chip-row {
  display: flex;
  gap: 8px;
  margin-top: 12px;
  overflow-x: auto;
  padding-bottom: 2px;
  scrollbar-width: none;
}

.chip-row::-webkit-scrollbar {
  display: none;
}

.content-area {
  position: absolute;
  top: calc(var(--wardrobe-top-gap) + var(--wardrobe-filter-height));
  right: var(--wardrobe-side-gap);
  bottom: var(--wardrobe-bottom-gap);
  left: var(--wardrobe-side-gap);
  overflow-y: auto;
  padding-top: 10px;
  padding-bottom: 12px;
  scroll-behavior: smooth;
  overscroll-behavior: contain;
}

.filter-chip,
.line-button {
  border: none;
  border-radius: 999px;
}

.filter-chip {
  flex: none;
  padding: 8px 14px;
  background: var(--accent-soft);
  color: var(--accent-strong);
  font-size: 11px;
  cursor: pointer;
}

.filter-chip.active {
  background: var(--accent-strong);
  color: var(--surface-strong);
}

.state-shell,
.wardrobe-card {
  border-radius: 24px;
  border: 1px solid var(--line-soft);
  background: var(--surface);
  box-shadow: var(--shadow-card);
}

.state-shell {
  margin-top: 8px;
  padding: 28px 20px;
  text-align: center;
  color: var(--text-soft);
  font-size: 12px;
}

.empty-actions {
  display: flex;
  justify-content: center;
  gap: 10px;
  margin-top: 14px;
  flex-wrap: wrap;
}

.line-button {
  padding: 10px 16px;
  background: var(--accent-soft);
  color: var(--accent-strong);
  font-size: 11px;
  cursor: pointer;
}

.import-button {
  background: color-mix(in srgb, var(--surface-strong) 88%, var(--accent-soft));
}

.line-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.wardrobe-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.wardrobe-card {
  overflow: hidden;
  cursor: pointer;
}

.hanger-shell {
  position: relative;
  aspect-ratio: 3 / 4;
  padding: 18px 18px 0;
  background: linear-gradient(180deg, color-mix(in srgb, var(--surface-strong) 90%, transparent), transparent);
}

.hanger-mark {
  position: absolute;
  top: 12px;
  left: 50%;
  width: 42px;
  height: 14px;
  margin-left: -21px;
  border: 1.4px solid var(--line-strong);
  border-bottom: none;
  border-radius: 18px 18px 0 0;
}

.hanger-shell img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 22px 22px 0 0;
}

.card-copy {
  padding: 12px 12px 14px;
}

.card-copy strong {
  color: var(--text-main);
  font-size: 12px;
  font-weight: 600;
}

.card-copy p {
  margin-top: 6px;
  font-size: 10px;
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 10px;
}

.tag-row span {
  color: var(--text-faint);
  font-size: 10px;
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 20px;
}

.fab-button {
  position: fixed;
  right: 20px;
  bottom: 88px;
  display: grid;
  place-items: center;
  width: 54px;
  height: 54px;
  border: 1px solid color-mix(in srgb, var(--accent-strong) 34%, transparent);
  border-radius: 50%;
  background: color-mix(in srgb, var(--accent-strong) 88%, var(--surface-strong));
  box-shadow: 0 18px 32px rgba(67, 52, 32, 0.22);
  cursor: pointer;
}

.fab-button span {
  position: absolute;
  width: 18px;
  height: 1.5px;
  background: var(--surface-strong);
}

.fab-button span:last-child {
  transform: rotate(90deg);
}

@media (min-width: 768px) {
  .wardrobe-page {
    inset: 0;
    --wardrobe-side-gap: 24px;
    --wardrobe-bottom-gap: 28px;
    padding: 0 24px 40px;
  }

  .filter-strip {
    padding-top: 18px;
  }

  .fab-button {
    bottom: 28px;
  }
}

@media (min-width: 1080px) {
  .wardrobe-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
</style>
