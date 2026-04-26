<template>
  <div class="detail-page">
    <header class="page-header">
      <button class="icon-back" type="button" aria-label="返回" @click="goBack">
        <span></span>
      </button>
      <div>
        <p class="eyebrow">首页轮播文案</p>
        <h1>管理文案</h1>
      </div>
    </header>

    <div v-if="isLoading" class="state-card">正在读取轮播文案...</div>

    <template v-else>
      <section class="card hero-card">
        <p class="section-caption">轮播状态</p>
        <h2>{{ activeCount }} 句启用中</h2>
        <p class="hero-note">左侧圆点表示是否参与首页轮播。可拖拽调整顺序，首页会按这里的顺序轮播。</p>
      </section>

      <section class="card">
        <div class="section-head">
          <p class="section-caption">手动添加</p>
          <h2>新增一句</h2>
        </div>

        <label class="field-block">
          <span>每句最多 20 个字</span>
          <div class="draft-row">
            <input
              v-model.trim="manualDraft"
              type="text"
              :maxlength="maxPromptLength"
              placeholder="比如：今天想穿成什么样"
              @keydown.enter.prevent="addManualPrompt"
            />
            <button class="ghost-button" type="button" @click="addManualPrompt">添加</button>
          </div>
          <small>{{ manualDraftLength }}/{{ maxPromptLength }}</small>
        </label>

        <button class="text-link" type="button" @click="goToImport">
          去 PDF 导入
        </button>
      </section>

      <section class="card">
        <div class="section-head">
          <p class="section-caption">分类</p>
          <h2>查看列表</h2>
        </div>

        <div class="filter-row">
          <button
            v-for="item in filterOptions"
            :key="item.value"
            :class="['filter-chip', activeFilter === item.value ? 'active' : '']"
            type="button"
            @click="activeFilter = item.value"
          >
            {{ item.label }}
          </button>
        </div>

        <div v-if="filteredItems.length" class="prompt-list">
          <article
            v-for="item in filteredItems"
            :key="item.id"
            :class="['prompt-item', item.enabled ? 'enabled' : '', draggingItemId === item.id ? 'dragging' : '', dragOverItemId === item.id ? 'drag-over' : '']"
            draggable="true"
            @dragstart="handleDragStart(item.id)"
            @dragover.prevent="handleDragOver(item.id)"
            @drop.prevent="handleDrop(item.id)"
            @dragend="handleDragEnd"
          >
            <button
              class="toggle-button"
              type="button"
              :aria-label="`${item.enabled ? '停用' : '启用'} ${item.text}`"
              @click="toggleEnabled(item.id)"
            >
              <span class="radio-dot"></span>
            </button>

            <div class="prompt-copy">
              <strong>{{ item.text }}</strong>
              <div class="meta-row">
                <span class="source-badge">{{ sourceLabelMap[item.sourceType] || '其他' }}</span>
                <span class="source-text">{{ item.sourceLabel }}</span>
              </div>
            </div>

            <div class="item-actions">
              <button class="move-button" type="button" aria-label="上移" @click="movePrompt(item.id, -1)">
                ↑
              </button>
              <button class="move-button" type="button" aria-label="下移" @click="movePrompt(item.id, 1)">
                ↓
              </button>
              <button class="drag-handle" type="button" aria-label="拖拽排序">
                ⋮⋮
              </button>
              <button class="delete-button" type="button" aria-label="删除" @click="removePrompt(item.id)">
                删除
              </button>
            </div>
          </article>
        </div>

        <div v-else class="empty-card">当前分类下还没有文案。</div>
      </section>

      <button class="primary-button" type="button" :disabled="isSaving" @click="saveSettings">
        {{ isSaving ? '保存中...' : '保存文案设置' }}
      </button>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { authApi } from '../api/auth';
import {
  buildHomePromptPayload,
  filterOptions,
  maxPromptLength,
  normalizeHomePromptSettings,
  normalizePromptItem,
  promptLength,
  sourceLabelMap,
  createLocalId
} from '../utils/homePromptSettings';

const router = useRouter();

const isLoading = ref(true);
const isSaving = ref(false);
const manualDraft = ref('');
const activeFilter = ref('all');
const settings = ref(normalizeHomePromptSettings());
const draggingItemId = ref('');
const dragOverItemId = ref('');

const items = computed(() => settings.value.items);
const activeCount = computed(() => items.value.filter((item) => item.enabled).length);
const manualDraftLength = computed(() => promptLength(manualDraft.value));
const filteredItems = computed(() => {
  if (activeFilter.value === 'all') {
    return items.value;
  }
  return items.value.filter((item) => item.sourceType === activeFilter.value);
});

const loadPage = async () => {
  isLoading.value = true;
  try {
    const response = await authApi.getHomePromptSettings();
    settings.value = normalizeHomePromptSettings(response.data || {});
  } catch (error) {
    console.error('Load prompt list failed:', error);
    alert(error.response?.data?.message || '读取轮播文案失败');
  } finally {
    isLoading.value = false;
  }
};

const addManualPrompt = () => {
  const text = String(manualDraft.value || '').trim();
  if (!text) {
    alert('先写一句文案');
    return;
  }
  if (promptLength(text) > maxPromptLength) {
    alert('每句文案不能超过 20 个字');
    return;
  }
  if (items.value.some((item) => item.text === text)) {
    alert('这句文案已经存在了');
    return;
  }

  const nextItem = normalizePromptItem({
    id: createLocalId(),
    text,
    sourceType: 'manual',
    sourceLabel: '手动添加',
    enabled: true
  });

  settings.value = {
    ...settings.value,
    items: [nextItem, ...settings.value.items]
  };
  manualDraft.value = '';
  activeFilter.value = 'manual';
};

const toggleEnabled = (id) => {
  const nextItems = settings.value.items.map((item) =>
    item.id === id ? { ...item, enabled: !item.enabled } : item
  );

  if (!nextItems.some((item) => item.enabled)) {
    alert('至少保留一条启用文案');
    return;
  }

  settings.value = {
    ...settings.value,
    items: nextItems
  };
};

const rebuildItemsFromFilteredOrder = (orderedFilteredIds) => {
  const filteredIdSet = new Set(filteredItems.value.map((item) => item.id));
  const itemMap = new Map(settings.value.items.map((item) => [item.id, item]));
  const orderedFilteredItems = orderedFilteredIds.map((id) => itemMap.get(id)).filter(Boolean);

  let filteredCursor = 0;
  return settings.value.items.map((item) => {
    if (!filteredIdSet.has(item.id)) {
      return item;
    }
    const nextItem = orderedFilteredItems[filteredCursor];
    filteredCursor += 1;
    return nextItem || item;
  });
};

const movePrompt = (id, offset) => {
  const visibleIds = filteredItems.value.map((item) => item.id);
  const currentIndex = visibleIds.indexOf(id);
  const nextIndex = currentIndex + offset;

  if (currentIndex < 0 || nextIndex < 0 || nextIndex >= visibleIds.length) {
    return;
  }

  const orderedIds = [...visibleIds];
  const [movedId] = orderedIds.splice(currentIndex, 1);
  orderedIds.splice(nextIndex, 0, movedId);

  settings.value = {
    ...settings.value,
    items: rebuildItemsFromFilteredOrder(orderedIds)
  };
};

const handleDragStart = (id) => {
  draggingItemId.value = id;
  dragOverItemId.value = id;
};

const handleDragOver = (id) => {
  if (!draggingItemId.value || draggingItemId.value === id) {
    return;
  }
  dragOverItemId.value = id;
};

const handleDrop = (id) => {
  const draggedId = draggingItemId.value;
  if (!draggedId || draggedId === id) {
    handleDragEnd();
    return;
  }

  const visibleIds = filteredItems.value.map((item) => item.id);
  const fromIndex = visibleIds.indexOf(draggedId);
  const toIndex = visibleIds.indexOf(id);

  if (fromIndex < 0 || toIndex < 0) {
    handleDragEnd();
    return;
  }

  const orderedIds = [...visibleIds];
  const [movedId] = orderedIds.splice(fromIndex, 1);
  orderedIds.splice(toIndex, 0, movedId);

  settings.value = {
    ...settings.value,
    items: rebuildItemsFromFilteredOrder(orderedIds)
  };
  handleDragEnd();
};

const handleDragEnd = () => {
  draggingItemId.value = '';
  dragOverItemId.value = '';
};

const removePrompt = (id) => {
  const nextItems = settings.value.items.filter((item) => item.id !== id);
  if (!nextItems.length) {
    alert('至少保留一条文案');
    return;
  }
  if (!nextItems.some((item) => item.enabled)) {
    nextItems[0].enabled = true;
  }
  settings.value = {
    ...settings.value,
    items: nextItems
  };
};

const saveSettings = async () => {
  isSaving.value = true;
  try {
    const response = await authApi.updateHomePromptSettings(buildHomePromptPayload(settings.value));
    settings.value = normalizeHomePromptSettings(response.data || {});
    alert('轮播文案已保存');
  } catch (error) {
    console.error('Save prompt list failed:', error);
    alert(error.response?.data?.message || '保存轮播文案失败');
  } finally {
    isSaving.value = false;
  }
};

const goBack = () => {
  router.push('/profile/system/home');
};

const goToImport = () => {
  router.push('/profile/system/home/import');
};

onMounted(loadPage);
</script>

<style scoped>
.detail-page {
  min-height: 100vh;
  padding: 20px 16px 40px;
  background:
    radial-gradient(circle at top, rgba(255, 252, 246, 0.92), transparent 28%),
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
  background: color-mix(in srgb, var(--surface-strong) 92%, transparent);
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
.state-card,
.empty-card {
  margin-top: 12px;
  border-radius: 22px;
  border: 1px solid var(--line-soft);
  background: color-mix(in srgb, var(--surface-strong) 93%, transparent);
  box-shadow: var(--shadow-card);
}

.card,
.state-card {
  padding: 16px;
}

.state-card,
.empty-card {
  text-align: center;
  color: var(--text-soft);
  font-size: 12px;
}

.empty-card {
  padding: 22px 16px;
  margin-top: 14px;
}

.hero-card {
  background:
    radial-gradient(circle at top left, rgba(214, 193, 164, 0.22), transparent 40%),
    color-mix(in srgb, var(--surface-strong) 94%, transparent);
}

.section-head h2,
.hero-card h2 {
  margin-top: 4px;
  font-size: 15px;
}

.hero-note {
  margin-top: 10px;
  color: var(--text-soft);
  font-size: 11px;
  line-height: 1.7;
}

.field-block {
  display: block;
  margin-top: 14px;
}

.field-block > span {
  display: block;
  color: var(--text-main);
  font-size: 13px;
}

.field-block small {
  display: block;
  margin-top: 8px;
  color: var(--text-faint);
  font-size: 10px;
}

.draft-row {
  display: flex;
  gap: 10px;
  margin-top: 10px;
}

.draft-row input {
  width: 100%;
  min-width: 0;
  padding: 12px 14px;
  border: 1px solid var(--line-soft);
  border-radius: 18px;
  background: color-mix(in srgb, var(--surface-strong) 95%, transparent);
  color: var(--text-main);
  font-size: 12px;
  outline: none;
}

.ghost-button,
.primary-button,
.toggle-button,
.delete-button,
.text-link,
.filter-chip {
  border: none;
  cursor: pointer;
}

.ghost-button,
.text-link {
  color: var(--accent-strong);
  background: transparent;
}

.ghost-button {
  flex: none;
  padding: 0 16px;
  border-radius: 999px;
  background: rgba(194, 168, 130, 0.14);
  font-size: 12px;
}

.text-link {
  margin-top: 12px;
  padding: 0;
  font-size: 12px;
}

.filter-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
}

.filter-chip {
  padding: 7px 14px;
  border: 1px solid var(--line-soft);
  border-radius: 999px;
  background: var(--surface-strong);
  color: var(--text-soft);
  font-size: 11px;
}

.filter-chip.active {
  border-color: rgba(160, 126, 82, 0.55);
  background: rgba(194, 168, 130, 0.16);
  color: var(--text-main);
}

.prompt-list {
  margin-top: 14px;
}

.prompt-item {
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 12px;
  padding: 14px 0;
  border-top: 1px solid var(--line-soft);
}

.prompt-item:first-of-type {
  border-top: none;
  padding-top: 4px;
}

.prompt-item.enabled .prompt-copy strong {
  color: var(--accent-strong);
}

.prompt-item.dragging {
  opacity: 0.56;
}

.prompt-item.drag-over {
  border-top-color: rgba(160, 126, 82, 0.58);
}

.toggle-button {
  display: inline-grid;
  place-items: center;
  width: 24px;
  height: 24px;
  background: transparent;
}

.radio-dot {
  width: 14px;
  height: 14px;
  border: 1.6px solid rgba(154, 126, 89, 0.62);
  border-radius: 50%;
  transition: all 0.2s ease;
}

.prompt-item.enabled .radio-dot {
  border-width: 4.5px;
  border-color: var(--accent-strong);
}

.prompt-copy strong {
  display: block;
  color: var(--text-main);
  font-size: 13px;
  line-height: 1.6;
}

.meta-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  margin-top: 6px;
}

.source-badge {
  padding: 3px 8px;
  border-radius: 999px;
  background: rgba(194, 168, 130, 0.14);
  color: var(--accent-strong);
  font-size: 10px;
}

.source-text {
  color: var(--text-faint);
  font-size: 10px;
}

.item-actions {
  display: flex;
  align-items: center;
  gap: 6px;
}

.move-button,
.drag-handle,
.delete-button {
  background: transparent;
}

.move-button,
.drag-handle {
  padding: 6px 4px;
  color: var(--text-faint);
  font-size: 12px;
}

.drag-handle {
  cursor: grab;
  letter-spacing: -0.12em;
}

.delete-button {
  padding: 8px 0 8px 10px;
  color: #8b6f56;
  font-size: 11px;
}

.primary-button {
  width: 100%;
  margin-top: 16px;
  padding: 11px 16px;
  border-radius: 999px;
  background: var(--accent-strong);
  color: var(--surface-strong);
  font-size: 12px;
}

.primary-button:disabled {
  opacity: 0.58;
  cursor: not-allowed;
}

@media (max-width: 640px) {
  .draft-row {
    flex-direction: column;
  }

  .ghost-button {
    height: 42px;
  }
}

@media (min-width: 768px) {
  .detail-page {
    padding: 28px 24px 40px;
  }
}
</style>
