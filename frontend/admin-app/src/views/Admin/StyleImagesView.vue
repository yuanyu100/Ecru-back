<template>
  <div class="admin-page style-images-page">
    <div class="stats-grid">
      <article class="stat-card">
        <span class="stat-label">分类数</span>
        <strong>{{ categoryCount }}</strong>
      </article>
      <article class="stat-card">
        <span class="stat-label">图片数</span>
        <strong>{{ items.length }}</strong>
      </article>
      <article class="stat-card">
        <span class="stat-label">启用中</span>
        <strong>{{ activeCount }}</strong>
      </article>
    </div>

    <section class="panel-card">
      <div class="panel-head">
        <h2>风格图库</h2>
        <button class="primary-button" type="button" @click="goCreate">新建风格图</button>
      </div>

      <div class="filter-bar">
        <input
          v-model.trim="filters.keyword"
          class="text-input filter-search"
          type="text"
          placeholder="搜索标题、来源或风格"
        />
        <select v-model="filters.active" class="text-input">
          <option value="">全部状态</option>
          <option :value="true">启用</option>
          <option :value="false">停用</option>
        </select>
        <div class="filter-actions">
          <button class="secondary-button" type="button" @click="resetFilters">重置</button>
          <button class="secondary-button" type="button" @click="loadImages">刷新</button>
        </div>
      </div>

      <div v-if="loading" class="empty-state">加载中...</div>
      <template v-else-if="categoryGroups.length">
        <div class="category-rail">
          <button
            v-for="group in categoryGroups"
            :key="group.name"
            :class="['category-pill', activeCategory === group.name ? 'active' : '']"
            type="button"
            @click="activeCategory = group.name"
          >
            <span>{{ group.label }}</span>
            <strong>{{ group.items.length }}</strong>
          </button>
        </div>

        <div v-if="activeGroup" class="gallery-shell">
          <div class="gallery-head">
            <strong>{{ activeGroup.label }}</strong>
            <span>{{ activeGroup.items.length }} 张图片</span>
          </div>

          <div class="gallery-grid">
            <article
              v-for="item in activeGroup.items"
              :key="item.id"
              class="gallery-card"
            >
              <div class="gallery-image">
                <img :src="item.imageUrl" :alt="item.title || '风格图片'" />
                <span class="gallery-status" :class="item.isActive ? 'is-active' : 'is-inactive'">
                  {{ item.isActive ? '启用' : '停用' }}
                </span>
              </div>

              <div class="gallery-copy">
                <strong>{{ item.title || '未命名图片' }}</strong>
                <p>{{ item.source || '未填写来源' }}</p>
                <div class="gallery-meta">
                  <span>{{ formatTags(item) }}</span>
                  <span>{{ formatDate(item.updatedAt || item.createdAt) }}</span>
                </div>
              </div>

              <div class="gallery-actions">
                <button class="mini-action" type="button" @click="goEdit(item)">编辑</button>
                <button
                  class="mini-action danger"
                  type="button"
                  :disabled="deletingId === item.id"
                  @click="removeItem(item)"
                >
                  {{ deletingId === item.id ? '删除中' : '删除' }}
                </button>
              </div>
            </article>
          </div>
        </div>
      </template>
      <p v-else class="empty-state">当前没有风格图片数据。</p>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { styleImageAdminApi } from '../../api/styleImage';

const router = useRouter();

const ALL_CATEGORY = '__all__';
const UNCATEGORIZED_CATEGORY = '__uncategorized__';

const items = ref([]);
const activeCategory = ref(ALL_CATEGORY);
const loading = ref(false);
const deletingId = ref(null);
const filters = reactive({ keyword: '', active: '' });

const normalizeText = (value) => (value || '').trim();
const isValidStyleLabel = (value) => {
  const text = normalizeText(value);
  if (!text) {
    return false;
  }
  return !/^[?\uff1f\uFFFD]+$/.test(text);
};

const splitCompositeCategories = (value) =>
  normalizeText(value)
    .split('/')
    .map((item) => item.trim())
    .filter(isValidStyleLabel);

const resolveGalleryCategories = (item = {}) => {
  const tagNames = Array.isArray(item.tags)
    ? item.tags.map((tag) => normalizeText(tag.name)).filter(isValidStyleLabel)
    : [];
  const styleCategories = splitCompositeCategories(item.styleCategory);
  const categories = [...new Set([...tagNames, ...styleCategories])];
  return categories.length ? categories : [UNCATEGORIZED_CATEGORY];
};

const categoryGroups = computed(() => {
  const grouped = new Map();

  items.value.forEach((item) => {
    resolveGalleryCategories(item).forEach((name) => {
      if (!grouped.has(name)) {
        grouped.set(name, []);
      }
      grouped.get(name).push(item);
    });
  });

  const groups = [...grouped.entries()]
    .map(([name, records]) => ({
      name,
      label: name === UNCATEGORIZED_CATEGORY ? '未分类' : name,
      items: records
    }))
    .sort((a, b) => a.label.localeCompare(b.label, 'zh-CN'));

  return [
    { name: ALL_CATEGORY, label: '全部', items: items.value },
    ...groups
  ];
});

const activeGroup = computed(() => categoryGroups.value.find((group) => group.name === activeCategory.value) || null);
const activeCount = computed(() => items.value.filter((item) => item.isActive).length);
const categoryCount = computed(() => Math.max(categoryGroups.value.length - 1, 0));

const ensureActiveCategory = () => {
  if (!categoryGroups.value.length) {
    activeCategory.value = '';
    return;
  }
  if (!categoryGroups.value.some((group) => group.name === activeCategory.value)) {
    activeCategory.value = ALL_CATEGORY;
  }
};

const formatDate = (value) => (value ? String(value).replace('T', ' ') : '-');

const formatTags = (item = {}) => {
  const names = resolveGalleryCategories(item)
    .filter((name) => name !== UNCATEGORIZED_CATEGORY);
  return names.length ? names.join(' / ') : '未分类';
};

const loadImages = async () => {
  loading.value = true;
  try {
    const result = await styleImageAdminApi.getStyleImages({
      page: 1,
      size: 200,
      keyword: filters.keyword,
      active: filters.active === '' ? undefined : filters.active
    });
    items.value = result?.data?.records || [];
    ensureActiveCategory();
  } finally {
    loading.value = false;
  }
};

const resetFilters = () => {
  filters.keyword = '';
  filters.active = '';
  loadImages();
};

const goCreate = () => router.push({ name: 'admin-style-image-create' });

const goEdit = (item) => {
  router.push({ name: 'admin-style-image-edit', params: { id: item.id }, state: { item } });
};

const removeItem = async (item) => {
  if (!window.confirm(`确认删除「${item.title || `图片 ${item.id}`}」吗？`)) {
    return;
  }

  deletingId.value = item.id;
  try {
    await styleImageAdminApi.deleteStyleImage(item.id);
    items.value = items.value.filter((current) => current.id !== item.id);
    ensureActiveCategory();
  } catch (error) {
    console.error('Delete style image failed:', error);
    alert(error.response?.data?.message || '删除风格图片失败');
  } finally {
    deletingId.value = null;
  }
};

onMounted(loadImages);
</script>

<style scoped>
.style-images-page {
  gap: 20px;
}

.empty-state {
  margin: 0;
  padding: 28px 0;
  color: #6e8092;
  text-align: center;
}

.category-rail {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 4px;
}

.category-pill {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  min-height: 40px;
  padding: 0 14px;
  border: 1px solid #d9e2eb;
  border-radius: 999px;
  background: #fff;
  color: #2f4153;
  cursor: pointer;
}

.category-pill strong {
  color: #6a7f95;
}

.category-pill.active {
  border-color: #93a9bf;
  background: linear-gradient(180deg, #edf4fa 0%, #e7f0f8 100%);
  color: #183149;
}

.gallery-shell {
  display: grid;
  gap: 14px;
}

.gallery-head {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.gallery-head strong {
  color: #1f3142;
  font-size: 18px;
}

.gallery-head span {
  color: #71869b;
  font-size: 13px;
}

.gallery-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 14px;
}

.gallery-card {
  display: grid;
  gap: 12px;
  padding: 14px;
  border: 1px solid #e6edf5;
  border-radius: 22px;
  background: #fff;
  transition: transform 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease;
}

.gallery-card:hover {
  transform: translateY(-1px);
  border-color: #9db1c6;
  box-shadow: 0 14px 28px rgba(35, 56, 78, 0.08);
}

.gallery-image {
  position: relative;
  aspect-ratio: 1 / 1.08;
  border-radius: 18px;
  overflow: hidden;
  background: #f7f4ef;
  border: 1px solid #e9edf2;
}

.gallery-image img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.gallery-status {
  position: absolute;
  top: 10px;
  right: 10px;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  backdrop-filter: blur(8px);
}

.gallery-status.is-active {
  background: rgba(229, 244, 234, 0.92);
  color: #1d6b39;
}

.gallery-status.is-inactive {
  background: rgba(244, 232, 232, 0.94);
  color: #8c3d3d;
}

.gallery-copy {
  display: grid;
  gap: 6px;
}

.gallery-copy strong {
  color: #213243;
}

.gallery-copy p {
  margin: 0;
  color: #6e8092;
  font-size: 13px;
}

.gallery-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  color: #7b8e9f;
  font-size: 12px;
}

.gallery-actions {
  display: flex;
  gap: 8px;
}

.mini-action {
  flex: 1;
  min-height: 34px;
  border: 1px solid #d7e0ea;
  border-radius: 999px;
  background: #f7fafc;
  color: #2e4256;
  cursor: pointer;
}

.mini-action.danger {
  color: #864848;
}
</style>
