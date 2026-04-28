<template>
  <div class="style-images-page">
    <section class="hero-card">
      <div class="hero-copy">
        <p class="hero-kicker">Style Direction Studio</p>
        <h2>风格方向图片库</h2>
        <p class="hero-text">这里不是堆数据，而是整理用户会感知到的风格方向。分类要好选，风格要一眼能看懂。</p>
      </div>

      <div class="hero-stats">
        <article class="stat-card">
          <span>分类数</span>
          <strong>{{ categoryGroups.length }}</strong>
        </article>
        <article class="stat-card">
          <span>图片数</span>
          <strong>{{ items.length }}</strong>
        </article>
        <article class="stat-card">
          <span>启用中</span>
          <strong>{{ activeCount }}</strong>
        </article>
      </div>
    </section>

    <section class="workspace">
      <div class="library-panel">
        <div class="panel-top">
          <div>
            <h3>图库浏览</h3>
            <p>先按分类看，再挑具体图改。</p>
          </div>

          <div class="toolbar">
            <input
              v-model.trim="filters.keyword"
              class="text-input"
              type="text"
              placeholder="搜索标题、来源或分类"
            />
            <select v-model="filters.active" class="text-input select-input">
              <option value="">全部状态</option>
              <option :value="true">启用</option>
              <option :value="false">停用</option>
            </select>
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
              <div>
                <strong>{{ activeGroup.label }}</strong>
                <span>{{ activeGroup.items.length }} 张图片</span>
              </div>
            </div>

            <div class="gallery-grid">
              <article
                v-for="item in activeGroup.items"
                :key="item.id"
                :class="['gallery-card', draft.id === item.id ? 'selected' : '']"
                @click="editItem(item)"
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
                  <button class="mini-action" type="button" @click.stop="editItem(item)">编辑</button>
                  <button
                    class="mini-action danger"
                    type="button"
                    :disabled="deletingId === item.id"
                    @click.stop="removeItem(item)"
                  >
                    {{ deletingId === item.id ? '删除中' : '删除' }}
                  </button>
                </div>
              </article>
            </div>
          </div>
        </template>
        <p v-else class="empty-state">当前没有风格图片数据。</p>
      </div>

      <div class="editor-panel">
        <div class="panel-top editor-top">
          <div>
            <h3>{{ draft.id ? '编辑风格图' : '新增风格图' }}</h3>
            <p>“风格分类”点开后直接选，不靠死记名字。</p>
          </div>
          <button class="primary-button" type="button" @click="resetDraft">新建一张</button>
        </div>

        <div class="editor-layout">
          <div class="preview-panel">
            <div class="preview-stage">
              <img v-if="draft.imageUrl" :src="draft.imageUrl" :alt="draft.title || '风格图片预览'" />
              <p v-else>图片预览区域</p>
            </div>

            <label class="upload-trigger">
              <span>{{ uploading ? '上传中...' : '上传风格图片' }}</span>
              <input type="file" accept="image/*" :disabled="uploading" @change="handleFileChange" />
            </label>

            <input
              v-model.trim="draft.imageUrl"
              class="text-input"
              type="text"
              placeholder="或直接填写图片 URL"
            />
          </div>

          <form class="editor-form" @submit.prevent="saveItem">
            <label>
              <span>图片标题</span>
              <input v-model.trim="draft.title" class="text-input" type="text" placeholder="例如：通勤极简示例" />
            </label>

            <div ref="stylePickerRef" class="style-field">
              <label>
                <span>风格分类</span>
                <button class="style-input-button" type="button" @click="toggleStylePicker">
                  <div class="style-input-copy">
                    <div v-if="selectedDraftTags.length" class="style-chip-list">
                      <span v-for="tag in selectedDraftTags" :key="tag.id" class="style-selection-chip">
                        {{ tag.name }}
                      </span>
                    </div>
                    <span v-else :class="['style-input-value', draft.styleCategory ? 'filled' : '']">
                      {{ draft.styleCategory || '点击选择风格，可多选' }}
                    </span>
                    <small class="style-input-hint">
                      {{ selectedDraftTags.length ? `已选 ${selectedDraftTags.length} 个风格` : '先选风格，未命名时会自动拼接成标题' }}
                    </small>
                  </div>
                  <span :class="['style-input-arrow', stylePickerOpen ? 'open' : '']"></span>
                </button>
              </label>

              <div v-if="stylePickerOpen" class="style-picker">
                <div class="style-picker-top">
                  <div>
                    <strong>选择风格</strong>
                    <p>像点选项一样直接选，可多选，已选结果会立刻回到上面。</p>
                  </div>
                  <input
                    v-model.trim="stylePickerKeyword"
                    class="text-input compact-input"
                    type="text"
                    placeholder="筛选风格"
                  />
                </div>

                <div v-if="filteredTagGroups.length" class="style-picker-body">
                  <section v-for="group in filteredTagGroups" :key="group.category" class="style-group">
                    <header class="style-group-head">
                      <strong>{{ group.category }}</strong>
                      <span>{{ group.tags.length }}</span>
                    </header>

                    <div class="style-chip-grid">
                      <button
                        v-for="tag in group.tags"
                        :key="tag.id"
                        :class="['style-chip', isTagSelected(tag.id) ? 'selected' : '']"
                        type="button"
                        @click="toggleStyleTag(tag)"
                      >
                        {{ tag.name }}
                      </button>
                    </div>
                  </section>
                  <div class="style-picker-foot">
                    <span>{{ draft.styleCategory || '还没有选择风格' }}</span>
                    <button class="mini-action" type="button" @click="closeStylePicker">完成选择</button>
                  </div>
                </div>
                <p v-else class="empty-picker">没有匹配的风格。</p>
              </div>
            </div>

            <label>
              <span>来源</span>
              <input v-model.trim="draft.source" class="text-input" type="text" placeholder="例如：演示数据 / 人工整理" />
            </label>

            <label>
              <span>来源链接</span>
              <input v-model.trim="draft.sourceUrl" class="text-input" type="url" placeholder="https://..." />
            </label>

            <div class="inline-fields">
              <label>
                <span>参考价格</span>
                <input v-model.number="draft.price" class="text-input" type="number" min="0" step="0.01" />
              </label>

              <label>
                <span>状态</span>
                <select v-model="draft.isActive" class="text-input select-input">
                  <option :value="true">启用</option>
                  <option :value="false">停用</option>
                </select>
              </label>
            </div>

            <div class="form-actions">
              <button class="primary-button wide-button" type="submit" :disabled="saving">
                {{ saving ? '保存中...' : draft.id ? '保存修改' : '创建图片' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import { styleImageAdminApi } from '../../api/styleImage';

const createEmptyDraft = () => ({
  id: null,
  imageUrl: '',
  title: '',
  source: '手工标注',
  sourceUrl: '',
  price: null,
  styleCategory: '',
  selectedTagIds: [],
  isActive: true
});

const items = ref([]);
const styleTags = ref([]);
const activeCategory = ref('');
const loading = ref(false);
const saving = ref(false);
const uploading = ref(false);
const deletingId = ref(null);
const stylePickerOpen = ref(false);
const stylePickerKeyword = ref('');
const stylePickerRef = ref(null);
const filters = reactive({
  keyword: '',
  active: ''
});
const draft = reactive(createEmptyDraft());

const normalizeCategory = (value) => (value || '').trim();
const normalizeTagIds = (values = []) =>
  [...new Set((Array.isArray(values) ? values : []).map((item) => Number(item)).filter((item) => Number.isFinite(item) && item > 0))];

const buildStyleSummaryFromTagIds = (tagIds = []) => {
  const names = normalizeTagIds(tagIds)
    .map((id) => styleTags.value.find((tag) => tag.id === id)?.name || '')
    .map((name) => normalizeCategory(name))
    .filter(Boolean);

  return [...new Set(names)].join(' / ');
};

const resolveGalleryCategory = (item = {}) => {
  const tagCategories = Array.isArray(item.tags)
    ? [...new Set(item.tags.map((tag) => normalizeCategory(tag.category)).filter(Boolean))]
    : [];

  return tagCategories[0] || normalizeCategory(item.styleCategory) || '__uncategorized__';
};

const categoryGroups = computed(() => {
  const grouped = new Map();

  items.value.forEach((item) => {
    const name = resolveGalleryCategory(item);
    if (!grouped.has(name)) {
      grouped.set(name, []);
    }
    grouped.get(name).push(item);
  });

  return [...grouped.entries()]
    .map(([name, records]) => ({
      name,
      label: name === '__uncategorized__' ? '未分类' : name,
      items: records
    }))
    .sort((left, right) => left.label.localeCompare(right.label, 'zh-CN'));
});

const activeGroup = computed(() => categoryGroups.value.find((group) => group.name === activeCategory.value) || null);

const activeCount = computed(() => items.value.filter((item) => item.isActive).length);
const selectedDraftTags = computed(() => {
  const tagMap = new Map(styleTags.value.map((tag) => [Number(tag.id), tag]));
  return normalizeTagIds(draft.selectedTagIds).map((id) => tagMap.get(id)).filter(Boolean);
});

const filteredTagGroups = computed(() => {
  const keyword = stylePickerKeyword.value.trim().toLowerCase();
  const grouped = new Map();

  styleTags.value.forEach((tag) => {
    const category = normalizeCategory(tag.category) || '其他';
    const name = normalizeCategory(tag.name);
    const matches =
      !keyword ||
      name.toLowerCase().includes(keyword) ||
      category.toLowerCase().includes(keyword);

    if (!matches) {
      return;
    }

    if (!grouped.has(category)) {
      grouped.set(category, []);
    }
    grouped.get(category).push(tag);
  });

  return [...grouped.entries()]
    .map(([category, tags]) => ({
      category,
      tags: tags.sort((left, right) => String(left.name).localeCompare(String(right.name), 'zh-CN'))
    }))
    .sort((left, right) => left.category.localeCompare(right.category, 'zh-CN'));
});

const ensureActiveCategory = () => {
  if (!categoryGroups.value.length) {
    activeCategory.value = '';
    return;
  }

  const exists = categoryGroups.value.some((group) => group.name === activeCategory.value);
  if (!exists) {
    activeCategory.value = categoryGroups.value[0].name;
  }
};

const formatDate = (value) => (value ? String(value).replace('T', ' ') : '-');

const formatPrice = (value) => {
  if (value === null || value === undefined || value === '') {
    return '-';
  }

  const numeric = Number(value);
  return Number.isNaN(numeric) ? String(value) : `¥${numeric.toFixed(2)}`;
};

const formatTags = (item = {}) => {
  if (item.styleCategory) {
    return item.styleCategory;
  }

  const names = Array.isArray(item.tags) ? item.tags.map((tag) => normalizeCategory(tag.name)).filter(Boolean) : [];
  return names.length ? [...new Set(names)].join(' / ') : '未分类';
};

const normalizePayload = () => ({
  imageUrl: draft.imageUrl.trim(),
  title: draft.title.trim() || null,
  source: draft.source.trim() || null,
  sourceUrl: draft.sourceUrl.trim() || null,
  price: draft.price === '' || draft.price === null || draft.price === undefined ? null : Number(draft.price),
  styleCategory: draft.styleCategory.trim(),
  styleTagIds: normalizeTagIds(draft.selectedTagIds),
  isActive: Boolean(draft.isActive)
});

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

const loadStyleTags = async () => {
  try {
    const result = await styleImageAdminApi.getStyleTags();
    styleTags.value = result?.data || [];
  } catch (error) {
    console.error('Load style tags failed:', error);
    styleTags.value = [];
  }
};

const resetDraft = () => {
  Object.assign(draft, createEmptyDraft());
  stylePickerKeyword.value = '';
  stylePickerOpen.value = false;
};

const editItem = (item) => {
  Object.assign(draft, {
    id: item.id,
    imageUrl: item.imageUrl || '',
    title: item.title || '',
    source: item.source || '',
    sourceUrl: item.sourceUrl || '',
    price: item.price ?? null,
    styleCategory: item.styleCategory || '',
    selectedTagIds: Array.isArray(item.tags) ? item.tags.map((tag) => Number(tag.id)).filter(Boolean) : [],
    isActive: Boolean(item.isActive)
  });
};

const toggleStylePicker = () => {
  stylePickerOpen.value = !stylePickerOpen.value;
  if (!stylePickerOpen.value) {
    stylePickerKeyword.value = '';
  }
};

const closeStylePicker = () => {
  stylePickerOpen.value = false;
  stylePickerKeyword.value = '';
};

const isTagSelected = (tagId) => normalizeTagIds(draft.selectedTagIds).includes(Number(tagId));

const toggleStyleTag = (tag) => {
  const current = new Set(normalizeTagIds(draft.selectedTagIds));
  const numericId = Number(tag.id);

  if (current.has(numericId)) {
    current.delete(numericId);
  } else {
    current.add(numericId);
  }

  draft.selectedTagIds = [...current];
  draft.styleCategory = buildStyleSummaryFromTagIds(draft.selectedTagIds) || draft.styleCategory.trim();
  if (!draft.selectedTagIds.length) {
    draft.styleCategory = '';
  }
};

const handleDocumentPointer = (event) => {
  if (!stylePickerRef.value) {
    return;
  }
  if (!stylePickerRef.value.contains(event.target)) {
    closeStylePicker();
  }
};

const saveItem = async () => {
  if (!draft.imageUrl.trim()) {
    alert('请先上传图片或填写图片 URL');
    return;
  }

  if (!draft.styleCategory.trim()) {
    alert('请选择风格分类');
    return;
  }

  saving.value = true;
  try {
    const payload = normalizePayload();
    if (draft.id) {
      await styleImageAdminApi.updateStyleImage(draft.id, payload);
    } else {
      await styleImageAdminApi.createStyleImage(payload);
    }

    await loadImages();
    resetDraft();
  } catch (error) {
    console.error('Save style image failed:', error);
    alert(error.response?.data?.message || '保存风格图片失败');
  } finally {
    saving.value = false;
  }
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
    if (draft.id === item.id) {
      resetDraft();
    }
  } catch (error) {
    console.error('Delete style image failed:', error);
    alert(error.response?.data?.message || '删除风格图片失败');
  } finally {
    deletingId.value = null;
  }
};

const handleFileChange = async (event) => {
  const file = event.target.files?.[0];
  event.target.value = '';

  if (!file) {
    return;
  }

  uploading.value = true;
  try {
    const result = await styleImageAdminApi.uploadStyleImage(file);
    draft.imageUrl = result?.data || '';
  } catch (error) {
    console.error('Upload style image failed:', error);
    alert(error.response?.data?.message || '上传风格图片失败');
  } finally {
    uploading.value = false;
  }
};

onMounted(async () => {
  document.addEventListener('mousedown', handleDocumentPointer);
  await Promise.all([loadImages(), loadStyleTags()]);
});

onBeforeUnmount(() => {
  document.removeEventListener('mousedown', handleDocumentPointer);
});
</script>

<style scoped>
.style-images-page {
  display: grid;
  gap: 18px;
}

.hero-card {
  display: grid;
  gap: 18px;
  padding: 26px;
  border: 1px solid #e5ebf2;
  border-radius: 28px;
  background:
    radial-gradient(circle at top left, rgba(215, 227, 239, 0.7), transparent 36%),
    linear-gradient(135deg, #fcfdfd 0%, #f5f8fb 58%, #eef3f7 100%);
}

.hero-kicker {
  margin: 0 0 8px;
  color: #6d8297;
  font-size: 12px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.hero-copy h2 {
  margin: 0;
  color: #1e2e3d;
  font-size: clamp(28px, 3vw, 40px);
  line-height: 1.06;
}

.hero-text {
  max-width: 620px;
  margin: 12px 0 0;
  color: #617386;
  line-height: 1.8;
}

.hero-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.stat-card {
  padding: 16px 18px;
  border: 1px solid rgba(206, 217, 228, 0.9);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.78);
  backdrop-filter: blur(8px);
}

.stat-card span {
  display: block;
  color: #6d7f92;
  font-size: 12px;
}

.stat-card strong {
  display: block;
  margin-top: 8px;
  color: #1f3142;
  font-size: 28px;
  font-weight: 600;
}

.workspace {
  display: grid;
  gap: 18px;
  align-items: start;
}

.library-panel,
.editor-panel {
  display: grid;
  gap: 18px;
  padding: 22px;
  border: 1px solid #e5ebf2;
  border-radius: 28px;
  background: #fbfcfd;
}

.panel-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.panel-top h3 {
  margin: 0;
  color: #1e2f40;
  font-size: 20px;
}

.panel-top p {
  margin: 8px 0 0;
  color: #698095;
}

.toolbar {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}

.toolbar .text-input {
  min-width: 180px;
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
  align-items: center;
  justify-content: space-between;
}

.gallery-head strong {
  color: #1f3142;
  font-size: 18px;
}

.gallery-head span {
  display: block;
  margin-top: 4px;
  color: #71869b;
  font-size: 13px;
}

.gallery-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.gallery-card {
  display: grid;
  gap: 12px;
  padding: 14px;
  border: 1px solid #e6edf5;
  border-radius: 22px;
  background: #fff;
  cursor: pointer;
  transition:
    transform 0.18s ease,
    border-color 0.18s ease,
    box-shadow 0.18s ease;
}

.gallery-card:hover,
.gallery-card.selected {
  transform: translateY(-1px);
  border-color: #9db1c6;
  box-shadow: 0 14px 28px rgba(35, 56, 78, 0.08);
}

.gallery-image {
  position: relative;
  aspect-ratio: 1 / 1.08;
  border-radius: 18px;
  overflow: hidden;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 244, 239, 0.98)),
    #fff;
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

.editor-panel {
  position: sticky;
  top: 18px;
}

.editor-top {
  align-items: center;
}

.editor-layout {
  display: grid;
  gap: 18px;
}

.preview-panel,
.editor-form {
  display: grid;
  gap: 14px;
}

.preview-stage {
  min-height: 360px;
  display: grid;
  place-items: center;
  border: 1px solid #e8edf3;
  border-radius: 24px;
  background:
    radial-gradient(circle at top, rgba(246, 248, 250, 0.82), transparent 40%),
    #fff;
  overflow: hidden;
}

.preview-stage img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.preview-stage p {
  color: #6d8092;
}

.upload-trigger {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 44px;
  padding: 0 18px;
  border-radius: 14px;
  background: #dfe9f4;
  color: #1d3955;
  cursor: pointer;
  overflow: hidden;
}

.upload-trigger input {
  position: absolute;
  inset: 0;
  opacity: 0;
  cursor: pointer;
}

.style-field {
  position: relative;
}

.style-input-button {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  width: 100%;
  min-height: 56px;
  padding: 12px 14px;
  border: 1px solid #d7e0ea;
  border-radius: 14px;
  background: #fff;
  cursor: pointer;
}

.style-input-copy {
  display: grid;
  gap: 8px;
  text-align: left;
}

.style-input-value {
  color: #90a0ae;
}

.style-input-value.filled {
  color: #223446;
}

.style-chip-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.style-selection-chip {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  background: #eef4fb;
  color: #22415f;
  font-size: 12px;
}

.style-input-hint {
  color: #7d90a2;
  font-size: 12px;
}

.style-input-arrow {
  margin-top: 10px;
  width: 9px;
  height: 9px;
  border-right: 1.5px solid #62788f;
  border-bottom: 1.5px solid #62788f;
  transform: rotate(45deg);
  transition: transform 0.2s ease;
}

.style-input-arrow.open {
  transform: rotate(225deg);
}

.style-picker {
  position: absolute;
  top: calc(100% + 10px);
  left: 0;
  right: 0;
  z-index: 20;
  padding: 16px;
  border: 1px solid #dde6ef;
  border-radius: 20px;
  background:
    linear-gradient(180deg, rgba(252, 253, 255, 0.98), rgba(246, 249, 252, 0.98)),
    #fff;
  box-shadow: 0 24px 48px rgba(26, 42, 59, 0.14);
}

.style-picker-top {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 180px;
  gap: 14px;
  align-items: end;
  margin-bottom: 14px;
}

.style-picker-top strong {
  color: #203244;
}

.style-picker-top p {
  margin: 6px 0 0;
  color: #6b8094;
  font-size: 13px;
}

.compact-input {
  min-height: 42px;
}

.style-picker-body {
  display: grid;
  gap: 14px;
  max-height: 320px;
  overflow: auto;
}

.style-group {
  display: grid;
  gap: 10px;
  padding: 12px;
  border: 1px solid #ecf1f6;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.78);
}

.style-group-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.style-group-head strong {
  color: #556c81;
  font-size: 13px;
}

.style-group-head span {
  color: #8ca0b3;
  font-size: 12px;
}

.style-chip-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.style-chip {
  min-height: 34px;
  padding: 0 12px;
  border: 1px solid #dbe3ec;
  border-radius: 999px;
  background: #f8fbfd;
  color: #304456;
  cursor: pointer;
}

.style-chip.selected {
  border-color: #96acc1;
  background: linear-gradient(180deg, #e9f2fa 0%, #e4eef8 100%);
  color: #1c334b;
}

.style-picker-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid #e8eef5;
  color: #5f758b;
  font-size: 13px;
}

.empty-picker {
  margin: 0;
  color: #708396;
  font-size: 13px;
}

.inline-fields {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.form-actions {
  padding-top: 6px;
}

.wide-button {
  width: 100%;
}

@media (min-width: 1180px) {
  .workspace {
    grid-template-columns: minmax(0, 1.15fr) minmax(360px, 0.85fr);
  }

  .editor-layout {
    grid-template-columns: minmax(210px, 280px) minmax(0, 1fr);
    align-items: start;
  }
}

@media (max-width: 900px) {
  .hero-stats,
  .gallery-grid,
  .inline-fields,
  .style-picker-top {
    grid-template-columns: 1fr;
  }

  .panel-top,
  .editor-top {
    flex-direction: column;
    align-items: stretch;
  }

  .toolbar {
    justify-content: flex-start;
  }

  .editor-panel {
    position: static;
  }
}
</style>
