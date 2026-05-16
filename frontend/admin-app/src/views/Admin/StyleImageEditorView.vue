<template>
  <div class="admin-page">
    <div class="detail-page-head">
      <button class="secondary-button" type="button" @click="$router.push('/style-images')">← 返回图库</button>
      <h2>{{ isEdit ? '编辑风格图' : '新建风格图' }}</h2>
    </div>

    <div v-if="isEdit && !itemData" class="panel-card">
      <p class="empty-tip">图片信息不可用，请从图库页进入。</p>
    </div>

    <section v-else class="panel-card">
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
          <input v-model.trim="draft.imageUrl" class="text-input" type="text" placeholder="或直接填写图片 URL" />
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
                    <span v-for="tag in selectedDraftTags" :key="tag.id" class="style-selection-chip">{{ tag.name }}</span>
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
                <strong>选择风格</strong>
                <input v-model.trim="stylePickerKeyword" class="text-input compact-input" type="text" placeholder="筛选风格" />
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
                    >{{ tag.name }}</button>
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
              <select v-model="draft.isActive" class="text-input">
                <option :value="true">启用</option>
                <option :value="false">停用</option>
              </select>
            </label>
          </div>

          <div class="form-actions">
            <button class="primary-button wide-button" type="submit" :disabled="saving">
              {{ saving ? '保存中...' : isEdit ? '保存修改' : '创建图片' }}
            </button>
          </div>
        </form>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { styleImageAdminApi } from '../../api/styleImage';

const route = useRoute();
const router = useRouter();

const isEdit = computed(() => Boolean(route.params.id));
const itemData = history.state?.item || null;

const createEmptyDraft = () => ({
  id: null, imageUrl: '', title: '', source: '手工标注',
  sourceUrl: '', price: null, styleCategory: '', selectedTagIds: [], isActive: true
});

const draft = reactive(isEdit.value && itemData ? {
  id: itemData.id,
  imageUrl: itemData.imageUrl || '',
  title: itemData.title || '',
  source: itemData.source || '',
  sourceUrl: itemData.sourceUrl || '',
  price: itemData.price ?? null,
  styleCategory: itemData.styleCategory || '',
  selectedTagIds: Array.isArray(itemData.tags) ? itemData.tags.map((t) => Number(t.id)).filter(Boolean) : [],
  isActive: Boolean(itemData.isActive)
} : createEmptyDraft());

const styleTags = ref([]);
const saving = ref(false);
const uploading = ref(false);
const stylePickerOpen = ref(false);
const stylePickerKeyword = ref('');
const stylePickerRef = ref(null);

const normalizeCategory = (value) => (value || '').trim();
const isValidStyleLabel = (value) => {
  const text = normalizeCategory(value);
  if (!text) return false;
  return !/^[?\uff1f\uFFFD]+$/.test(text);
};
const normalizeTagIds = (values = []) =>
  [...new Set((Array.isArray(values) ? values : []).map((v) => Number(v)).filter((v) => Number.isFinite(v) && v > 0))];

const buildStyleSummary = (tagIds = []) => {
  const names = normalizeTagIds(tagIds)
    .map((id) => styleTags.value.find((t) => t.id === id)?.name || '')
    .map(normalizeCategory).filter(isValidStyleLabel);
  return [...new Set(names)].join(' / ');
};

const selectedDraftTags = computed(() => {
  const tagMap = new Map(styleTags.value.map((t) => [Number(t.id), t]));
  return normalizeTagIds(draft.selectedTagIds).map((id) => tagMap.get(id)).filter(Boolean);
});

const filteredTagGroups = computed(() => {
  const keyword = stylePickerKeyword.value.trim().toLowerCase();
  const grouped = new Map();
  styleTags.value.forEach((tag) => {
    const category = isValidStyleLabel(tag.category) ? normalizeCategory(tag.category) : '其他';
    const name = normalizeCategory(tag.name);
    if (!isValidStyleLabel(name)) return;
    if (keyword && !name.toLowerCase().includes(keyword) && !category.toLowerCase().includes(keyword)) return;
    if (!grouped.has(category)) grouped.set(category, []);
    grouped.get(category).push(tag);
  });
  return [...grouped.entries()]
    .map(([category, tags]) => ({ category, tags: tags.sort((a, b) => String(a.name).localeCompare(String(b.name), 'zh-CN')) }))
    .sort((a, b) => a.category.localeCompare(b.category, 'zh-CN'));
});

const isTagSelected = (tagId) => normalizeTagIds(draft.selectedTagIds).includes(Number(tagId));

const toggleStyleTag = (tag) => {
  const current = new Set(normalizeTagIds(draft.selectedTagIds));
  const id = Number(tag.id);
  current.has(id) ? current.delete(id) : current.add(id);
  draft.selectedTagIds = [...current];
  draft.styleCategory = buildStyleSummary(draft.selectedTagIds) || draft.styleCategory.trim();
  if (!draft.selectedTagIds.length) draft.styleCategory = '';
};

const toggleStylePicker = () => { stylePickerOpen.value = !stylePickerOpen.value; if (!stylePickerOpen.value) stylePickerKeyword.value = ''; };
const closeStylePicker = () => { stylePickerOpen.value = false; stylePickerKeyword.value = ''; };

const handleDocumentPointer = (event) => {
  if (stylePickerRef.value && !stylePickerRef.value.contains(event.target)) closeStylePicker();
};

const handleFileChange = async (event) => {
  const file = event.target.files?.[0];
  event.target.value = '';
  if (!file) return;
  uploading.value = true;
  try {
    const result = await styleImageAdminApi.uploadStyleImage(file);
    draft.imageUrl = result?.data || '';
  } catch (error) {
    alert(error.response?.data?.message || '上传风格图片失败');
  } finally {
    uploading.value = false;
  }
};

const saveItem = async () => {
  if (!draft.imageUrl.trim()) { alert('请先上传图片或填写图片 URL'); return; }
  if (!draft.styleCategory.trim()) { alert('请选择风格分类'); return; }
  saving.value = true;
  try {
    const payload = {
      imageUrl: draft.imageUrl.trim(),
      title: draft.title.trim() || null,
      source: draft.source.trim() || null,
      sourceUrl: draft.sourceUrl.trim() || null,
      price: draft.price == null || draft.price === '' ? null : Number(draft.price),
      styleCategory: draft.styleCategory.trim(),
      styleTagIds: normalizeTagIds(draft.selectedTagIds),
      isActive: Boolean(draft.isActive)
    };
    if (isEdit.value) {
      await styleImageAdminApi.updateStyleImage(draft.id, payload);
    } else {
      await styleImageAdminApi.createStyleImage(payload);
    }
    router.push('/style-images');
  } catch (error) {
    alert(error.response?.data?.message || '保存风格图片失败');
  } finally {
    saving.value = false;
  }
};

onMounted(async () => {
  document.addEventListener('mousedown', handleDocumentPointer);
  try {
    const result = await styleImageAdminApi.getStyleTags();
    styleTags.value = (result?.data || []).filter((tag) => isValidStyleLabel(tag?.name));
  } catch (error) {
    console.error('Load style tags failed:', error);
  }
});

onBeforeUnmount(() => document.removeEventListener('mousedown', handleDocumentPointer));
</script>

<style scoped>
.editor-layout {
  display: grid;
  gap: 24px;
}

@media (min-width: 900px) {
  .editor-layout {
    grid-template-columns: minmax(210px, 280px) minmax(0, 1fr);
    align-items: start;
  }
}

.preview-panel,
.editor-form {
  display: grid;
  gap: 14px;
}

.preview-stage {
  min-height: 320px;
  display: grid;
  place-items: center;
  border: 1px solid #e8edf3;
  border-radius: 24px;
  background: #fff;
  overflow: hidden;
}

.preview-stage img { width: 100%; height: 100%; object-fit: contain; }
.preview-stage p { color: #6d8092; }

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

.upload-trigger input { position: absolute; inset: 0; opacity: 0; cursor: pointer; }

.style-field { position: relative; }

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

.style-input-copy { display: grid; gap: 8px; text-align: left; }
.style-input-value { color: #90a0ae; }
.style-input-value.filled { color: #223446; }
.style-chip-list { display: flex; flex-wrap: wrap; gap: 8px; }
.style-selection-chip {
  display: inline-flex; align-items: center; min-height: 28px; padding: 0 10px;
  border-radius: 999px; background: #eef4fb; color: #22415f; font-size: 12px;
}
.style-input-hint { color: #7d90a2; font-size: 12px; }
.style-input-arrow {
  margin-top: 10px; width: 9px; height: 9px;
  border-right: 1.5px solid #62788f; border-bottom: 1.5px solid #62788f;
  transform: rotate(45deg); transition: transform 0.2s ease;
}
.style-input-arrow.open { transform: rotate(225deg); }

.style-picker {
  position: absolute; top: calc(100% + 10px); left: 0; right: 0; z-index: 20;
  padding: 16px; border: 1px solid #dde6ef; border-radius: 20px;
  background: rgba(252, 253, 255, 0.98);
  box-shadow: 0 24px 48px rgba(26, 42, 59, 0.14);
}

.style-picker-top {
  display: flex; align-items: center; justify-content: space-between;
  gap: 14px; margin-bottom: 14px;
}
.style-picker-top strong { color: #203244; }
.compact-input { min-height: 42px; flex: 0 0 180px; }

.style-picker-body { display: grid; gap: 14px; max-height: 320px; overflow: auto; }
.style-group { display: grid; gap: 10px; padding: 12px; border: 1px solid #ecf1f6; border-radius: 16px; background: rgba(255,255,255,0.78); }
.style-group-head { display: flex; align-items: center; justify-content: space-between; }
.style-group-head strong { color: #556c81; font-size: 13px; }
.style-group-head span { color: #8ca0b3; font-size: 12px; }
.style-chip-grid { display: flex; flex-wrap: wrap; gap: 8px; }
.style-chip {
  min-height: 34px; padding: 0 12px; border: 1px solid #dbe3ec;
  border-radius: 999px; background: #f8fbfd; color: #304456; cursor: pointer;
}
.style-chip.selected { border-color: #96acc1; background: linear-gradient(180deg, #e9f2fa 0%, #e4eef8 100%); color: #1c334b; }
.style-picker-foot {
  display: flex; align-items: center; justify-content: space-between;
  gap: 12px; margin-top: 14px; padding-top: 14px;
  border-top: 1px solid #e8eef5; color: #5f758b; font-size: 13px;
}
.empty-picker { margin: 0; color: #708396; font-size: 13px; }

.inline-fields { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px; }
.form-actions { padding-top: 6px; }
.wide-button { width: 100%; }

.mini-action {
  flex: 1; min-height: 34px; border: 1px solid #d7e0ea;
  border-radius: 999px; background: #f7fafc; color: #2e4256; cursor: pointer;
}
</style>
